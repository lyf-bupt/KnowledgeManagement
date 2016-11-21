package com.bupt.poirot.main.jetty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.bupt.poirot.data.DealData;
import com.bupt.poirot.data.modelLibrary.FetchModelClient;
import com.bupt.poirot.z3.parseAndDeduceOWL.OWLToZ3;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import org.apache.jena.atlas.json.JsonObject;

import com.bupt.poirot.main.Config;
import org.apache.kafka.common.utils.Time;

public class Client {
	public Context context;
	public Solver solver;

	public static DateFormat formater = new SimpleDateFormat("yyyy/mm/dd hh:mm:ss");

	public DealData dealData;
	public LinkedList<JsonObject> resultQueue;
	public LinkedList<String> data;
	public String target;
	public String domain;
	public String topic;
	public String roadName;
	public RoadData roadData;
	public InputStream inputStream;

	public static HashMap<String, RoadData> stringRoadDataHashMap = new HashMap<>();

	static {
		stringRoadDataHashMap.put("翠竹路", new RoadData(114.134266, 22.582957, 114.134606, 22.580431));
		stringRoadDataHashMap.put("红岭中路", new RoadData(114.110848, 22.568226, 114.110848, 22.561985));
		stringRoadDataHashMap.put("福中路", new RoadData(114.056446, 22.548233, 114.059447, 22.548283));
		stringRoadDataHashMap.put("金田路", new RoadData(114.069633, 22.553857, 114.069562, 22.54835));
	}

	private TimeData timeData;

	public Client(Map<String, String[]> paramsMap) {
		if (paramsMap == null) {
			System.out.println("NULL");
		}
		target = paramsMap.get("target")[0];
		topic = paramsMap.get("topic")[0];
		domain = paramsMap.get("topic")[0];
		System.out.println("target : " + target + " topic : " + topic + " domain : " + domain);

		context = new Context();
		solver = context.mkSolver();
		Params params = context.mkParams();
		params.add("mbqi", false);
		solver.setParameters(params);

		if (topic == null) {
			System.out.println("need topic info");
			throw new RuntimeException("need topic info");
		}

		roadName = paramsMap.get("road")[0];
		roadData = stringRoadDataHashMap.get(roadName);
		System.out.println(roadData.x1 + "  " + roadData.y1 + "  " + roadData.x2 + "  " + roadData.y2);

		timeData = parseTimeSection(paramsMap.get("time")[0]);
		this.resultQueue = new LinkedList<>();
		this.data = new LinkedList<>();
		workflow();
	}

	private TimeData parseTimeSection(String time) {
		int num = Integer.valueOf(time.substring(0, time.length() - 1));
		long begin = 0;
		try {
			begin = formater.parse("2011/04/18 17:00:00").getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long end = begin;
		if (time.endsWith("s")) {
			end += num * 1000000;
		} if (time.endsWith("m")) {
			end += num * 60 * 1000000;
		}
		TimeData timeData = new TimeData(begin, end);
		return timeData;
	}

	public void workflow() {
		System.out.println("Fetch Model begin");
		fetchModel(domain);
		System.out.println("Fetch Model done");
		BoolExpr preAxiom = OWLToZ3.parseFromStream(context, inputStream);
		// 预先定义的公理 --- z3模式
		System.out.println("preAxiom : " + preAxiom);

		BoolExpr t = parseTarget(target);
		deduce(preAxiom, t, roadData);
	}

	private void fetchModel(String domain) {
		FetchModelClient fetchModelClient = new FetchModelClient();
		String host = "http://localhost:3030/";
		String query = ""; // TODO
		inputStream = fetchModelClient.fetch(host, domain, query);
		if (inputStream == null) {
			throw new RuntimeException("fetch model failed");
		}
	}


	private BoolExpr parseTarget(String target) {
		BoolExpr res = null;
		String[] strs = target.split("&");
		for (String str : strs) {
			// TODO
		}
		if (res == null) {
			return context.mkTrue();
		}
		return res;
	}

	private void deduce(BoolExpr preAxiom, BoolExpr t, RoadData roadData) {
		// TODO 推理后的结果存入resultQueue中
		boolean res = dealWithData(preAxiom, t, roadData);

//		solver.add(preAxiom);
//		if (t != null) {
//			solver.add(context.mkNot(t));
//		} else {
//			throw new RuntimeException("target is null");
//		}
//		boolean res;
//		if (solver.check() == Status.UNSATISFIABLE) {
//			res = true;
//		} else {
//			res = false;
//		}

		System.out.println("deduce done");
		JsonObject jsonObject = new JsonObject();
		if (res) {
			jsonObject.put("result", "yes");
		} else {
			jsonObject.put("result", "no");
		}
		resultQueue.addLast(jsonObject);
	}

	boolean isInTheRoad(double x, double y, RoadData roadData) {
		double m = (roadData.x1 - x) * (roadData.y2 - y);
		double n = (roadData.x2 - x) * (roadData.y1 - y);
		double result = m * n;
		if (Math.abs(result) > 0.000000000001) {
			return false;
		}

		if ((roadData.x1 - x) * (roadData.x2 - x) + (roadData.y1 - y) * (roadData.y2 - y) > 0) {
			return false;
		}

		double ax = x - roadData.x1;
		double ay = y - roadData.y1;

		double bx = roadData.x2 - roadData.x1;
		double by = roadData.y2 - roadData.x2;

		double aLength = Math.sqrt(ax * ax + ay * ay);
		double bLength = Math.sqrt(bx * bx + by * by);

		double cos = aLength * bLength / (ax * bx + ay * by);
		double sin = Math.sqrt(1.0 - cos * cos);

		double distane = aLength * sin;

		if (distane > 0.0000001) {
			return false;
		}

		return true;
	}

	private boolean dealWithData(BoolExpr preAxiom, BoolExpr target, RoadData roadData) {
		boolean res = false;
		File file = new File(Config.getValue("mac"));

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				count++;
				if (count > 10000000) {
					break;
				}
				if (count % 1000000 == 0) {
					System.out.println("dealt lines : " + count);
				}

				if (line == null || line.length() == 0) {
					continue;
				}
				String[] strs = line.split(",");
				if (strs.length < 7) {
					continue;
				}
				String time = strs[1]; // 时间
				long t;
				try {
					t = formater.parse(time).getTime();
				} catch (ParseException e1) {
					continue;
				}

				if (!isInTimeSection(t, timeData)) {
					continue;
				}

//				String carName = strs[0]; // 车名
//				boolean states = strs[4].equals("1") ? true : false; // 状态
				float x, y, speed;
				byte direction;
				try {
					x = Float.parseFloat(strs[2]); // 经度
					y = Float.parseFloat(strs[3]); // 纬度
					speed = Float.parseFloat(strs[5]); // 速度
					direction = Byte.parseByte(strs[6]); // 方向
				} catch (Exception e) {
					continue;
				}
				if (y <= 10 || y >= 40 || x <= 100 || x >= 140) {
					continue;
				}
				if (!isInTheRoad(x, y, roadData)) {
					continue;
				}

				System.out.println("got one x : " + x + "  y : " + y);
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		return res;
	}

	private boolean isInTimeSection(long t, TimeData timeData) {
		if (t < timeData.begin || t > timeData.end) {
			return false;
		}
		return true;
	}


	public JsonObject getResult() {
//		return dealData.gerOne();
		return resultQueue.removeFirst();
	}

	
//	public void accept() {
//		File file = new File(Config.getValue("mac"));
//		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
//            String line;
//            int count = 0;
//			while ((line = reader.readLine()) != null) {
//				data.addLast(line);
//                count++;
//                if (count > 100000) {
//                	return;
//                }
//                if (data.size() >= 10000) {
//                	deduceWithOneSection();
//					data.clear();
//
//                }
//            }
//		} catch (Exception e ) {
//			e.printStackTrace();
//		}
//	}

	private void deduceWithOneSection() {
		for (String message : data) {
			dealData.deal(message);
		}
		
		dealData.deduce();
	}

	public static void main(String[] args) {
//		System.out.println("begin accept");
		
//		HashMap<String, String> stringRoadDataHashMap = new HashMap<>();
//		String values = "10";
//		stringRoadDataHashMap.put("sections", values);
//		new Client(stringRoadDataHashMap);
		
	}
	
	public void acceptSocket() {
//		try (Socket socket = new Socket("127.0.0.1", 30000)) {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//			String message = null;
//			while ((message = reader.readLine()) != null) {
////				System.out.println(message);
//				dealData.deal(message);
//				Thread.sleep(1000);
//			}
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		}
	}
}
