package com.bupt.poirot.z3.deduce;

import com.bupt.poirot.jettyServer.jetty.TargetInfo;
import com.bupt.poirot.jettyServer.jetty.TimeData;
import com.bupt.poirot.knowledgeBase.incidents.Incident;
import com.bupt.poirot.knowledgeBase.incidents.IncidentFactory;
import com.bupt.poirot.knowledgeBase.schemaManage.IncidentToKnowledge;
import com.bupt.poirot.knowledgeBase.schemaManage.Knowledge;
import com.bupt.poirot.knowledgeBase.schemaManage.Position;
import com.bupt.poirot.knowledgeBase.incidents.TrafficIncident;
import com.bupt.poirot.utils.Config;
import com.microsoft.z3.Context;
import org.apache.jena.atlas.RuntimeIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Client {

	public static String IRI = "http://www.semanticweb.org/traffic-ontology#";
	private static DateFormat formater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public Context context;
	public RequestContext requestContext;
	public Deducer deducer;
	IncidentToKnowledge incidentToKnowledge;

	static int count = 0;

	public Client(TargetInfo targetInfo) {
		int id = Integer.valueOf(targetInfo.infos.get("id"));
		System.out.println("id : " + id);
		String scope = targetInfo.infos.get("scope");
		System.out.println("scope : " + scope);
		String topic = targetInfo.infos.get("topic");
		System.out.println("topic : " + topic);
		String min = targetInfo.infos.get("min");
		System.out.println("min : " + min);
		String a = targetInfo.infos.get("severe");
		System.out.println("severe : " + a);
		String b = targetInfo.infos.get("medium");
		System.out.println("medium : " + b);
		String c = targetInfo.infos.get("slight");
		System.out.println("slight : " + c);
		String speed = targetInfo.infos.get("speed");
		System.out.println("speed : " + speed);
//		System.out.println(id + " " + scope + " " + topic + " " + minCars + " " + a + " " + b + " " + c + " " + speed);

		this.requestContext = new RequestContext(id, topic, scope, min, a, b, c, speed);
		this.context = new Context();
		this.deducer = new Deducer(context, requestContext);
	}

	public void workflow() {
		System.out.println("begin workflow");
		init();
		acceptData();
	}

	public void init() {
        // TODO
		incidentToKnowledge = new IncidentToKnowledge();
		incidentToKnowledge.load();
	}

	public void acceptData() { // 数据
		File file = new File(Config.getString("data_file"));
		System.out.println(file.getAbsoluteFile());
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				count++;
				if (count % 1000000 == 0) {
					System.out.println("dealt lines : " + count);
				}
				deal(line, "traffic");
			}
		} catch (Exception e ) {
			e.printStackTrace();
		}
		System.out.println("dealt done");
	}

	private void deal(String message, String domain) {
		Knowledge knowledge = null;
		IncidentFactory incidentFactory = new IncidentFactory();
		Incident incident = incidentFactory.converIncident(domain, message);

		if (incident != null) {
			knowledge = getKnowledge(incident);// todo 根据事件对象映射成位置（知识库中已有的知识)
		}
		if (knowledge != null) {
			System.out.println(knowledge.getIRI());
			deducer.deduce(knowledge, incident);
		} else {
			incident = null;
			count++;
			if (count % 1000000 == 0) {
				System.gc();
			}
		}
	}


	private Knowledge getKnowledge(Incident incident) {
		Position position = null;
		if (incident instanceof TrafficIncident) {
			TrafficIncident trafficIncident = (TrafficIncident) incident;
			for (Position p : incidentToKnowledge.positionStringMap.keySet()) {
				if (trafficIncident.x >= p.x1 && trafficIncident.x <= p.x2 && trafficIncident.y >= p.y2 && trafficIncident.y <= p.y1) {
					position = p;
					break;
				}
			}
		}
		return position;
	}

	private TimeData parseTimeSection(String timeSection) {
		System.out.println("Time section : " + timeSection);
		String[] times = timeSection.split(" - ");
		if (times.length < 2) {
			throw new RuntimeIOException();
		}
		long begin = 0;
		long end = begin;
		try {
			begin = formater.parse(times[0]).getTime();
			end = formater.parse(times[1]).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		TimeData timeData = new TimeData(begin, end);
		return timeData;
	}

	public static void main(String[] args) {

	}
}
