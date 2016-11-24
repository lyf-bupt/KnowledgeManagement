package com.bupt.poirot.main.jetty;

import com.bupt.poirot.main.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Date;

/**
 * Created by hui.chen on 11/24/16.
 */
public class PreDealData {

    public static boolean transformFileEncoding(File file, String sourEncoding, String destEncoding) {
//        System.out.println(file.getName());

        String name = file.getName().split("\\.")[0];
        String suffix = file.getName().split("\\.")[1];
        File outputFile = new File(file.getParent() + File.pathSeparator + name + "_bak" + "\\." + suffix);

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), sourEncoding));
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            PrintStream printStream = new PrintStream(new FileOutputStream(outputFile), true, destEncoding);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                printStream.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        file.delete();
        outputFile.renameTo(file);
        return true;
    }

    public static void main(String[] args) {

//        transformFileEncoding(new File("粤B000H6.txt"),"gbk", "utf-8");
        Date begin = new Date();
        System.out.println("begin at :" + begin);
        File file = new File("./data/all_data_gbk.txt");
        for (File f : file.listFiles()) {
            transformFileEncoding(f, "gbk", "utf-8");
        }

        Date end = new Date();
        System.out.println("cost " + (end.getTime() - begin.getTime()) / 1000 + "s");

//        File file = new File(Config.getValue("data_file"));
//        System.out.println(file.exists());
//
//        String[] outputFileNames = new String[]{"20110418.txt", "20110419.txt", "20110420.txt", "20110421.txt","20110422.txt",
//                "20110423.txt", "20110424.txt", "20110425.txt", "20110426.txt"};
//
//        String[] marks = new String[]{"2011/04/18", "2011/04/19", "2011/04/20.txt", "2011/04/21","2011/04/22",
//                "2011/04/23", "2011/04/24", "2011/04/25", "2011/04/26"};
//
//
//
//        try {
//            File[] outputFiles = new File[outputFileNames.length];
//            PrintStream[] printStreams = new PrintStream[outputFileNames.length];
//            for (int i = 0; i < outputFileNames.length; i++) {
//                outputFiles[i] = new File(file.getParent() + File.separator + outputFileNames[i]);
//                System.out.println(outputFiles[i].getAbsoluteFile());
//
//                System.out.println(marks[i]);
//                if (!outputFiles[i].exists()) {
//                    outputFiles[i].createNewFile();
//                }
//                printStreams[i] = new PrintStream(new FileOutputStream(outputFiles[i]), true, "utf-8");
//            }
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
//
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                for (int i = 0; i < marks.length; i++) {
//                    if (line.contains(marks[i])) {
////                        System.out.println(line);
//                        printStreams[i].println(line);
//                        break;
//                    }
//                }
//            }
//        } catch (FileNotFoundException exception) {
//            exception.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
