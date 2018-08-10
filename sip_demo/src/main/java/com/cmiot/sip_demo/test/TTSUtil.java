//package com.cmiot.sip_demo.test;
//
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpStatus;
//import org.apache.commons.httpclient.methods.PostMethod;
//import org.apache.commons.httpclient.methods.RequestEntity;
//import org.apache.commons.httpclient.methods.StringRequestEntity;
//import org.apache.commons.io.IOUtils;
//
////import sun.org.mozilla.javascript.internal.ast.Block;
//
//import java.io.BufferedInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//
///**
// * Created by dbq on 2017/8/15.
// */
//public class TTSUtil {
//
//    private static String URL = "http://119.23.201.225:8880/tts/synthtext";
//    private static String responseString;
//
//    private static String app_key = "ac5d5452";
//    private static String dev_key = "developer_key";
//    private static String cap_key = "tts.cloud.synth";
//    private static String property = "cn_haobo_common";
//
//    private BlockingQueue<byte[]> queue = new ArrayBlockingQueue(20);
//    
//    public static BlockingQueue<Integer> integers = new ArrayBlockingQueue<Integer>(5);
//    
//    
//    private TTSUtil() {
//
//    }
//
//    public static TTSUtil getInstance() {
//        return Inner.instance;
//    }
//
//    public boolean put(String text) {
//        HttpClient client = new HttpClient();
//        PostMethod myPost = new PostMethod(URL);
//        BufferedInputStream bis = null;
//        ByteArrayOutputStream bos = null;
//        try {
//            myPost.setRequestHeader("Content-Type", "text/xml");
//            myPost.setRequestHeader("charset", "utf-8");
//            myPost.setRequestHeader("x-app-key", app_key);
//            myPost.setRequestHeader("x-sdk-version", "3.8");
//
//            String date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
//            myPost.setRequestHeader("x-request-date", date);
//            myPost.setRequestHeader("x-task-config", "capkey=" + cap_key + ", property=" + property + ",audioformat=alaw8k8bit,pitch=5,volume=5,speed=5");
//            String str = date + dev_key;
//            myPost.setRequestHeader("x-session-key", MD5.getMD5(str.getBytes()));
//            myPost.setRequestHeader("x-udid", "101:123456789");
//
//            RequestEntity entity = new StringRequestEntity(text, "text/html", "utf-8");
//            myPost.setRequestEntity(entity);
//            int statusCode = client.executeMethod(myPost);
//
//            if (statusCode == HttpStatus.SC_OK) {
//                bis = new BufferedInputStream(myPost.getResponseBodyAsStream());
//                byte[] bytes = new byte[1024];
//                bos = new ByteArrayOutputStream();
//                int count = 0;
//                while ((count = bis.read(bytes)) != -1) {
//                    bos.write(bytes, 0, count);
//                }
//                byte[] strByte = bos.toByteArray();
//                responseString = new String(strByte, 0, strByte.length, "iso-8859-1");
//                responseString = responseString.substring(responseString.indexOf("</ResponseInfo>") + 15);
//                strByte = responseString.getBytes("iso-8859-1");
//                
//                File file = new File("E:\\TTS.pcm");
//                FileOutputStream outputStream = null;
//                try {
//					outputStream = new FileOutputStream(file);
//					outputStream.write(strByte);
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//                //queue.offer(strByte);
//                return true;
//            }
//
//        } catch (Exception e) {
//        }
//        finally{
//            IOUtils.closeQuietly(bis);
//            IOUtils.closeQuietly(bos);
//        }
//        return false;
//    }
//
//    public byte[] get()
//    {
//        try {
//            return queue.take();
//        } catch (InterruptedException e) {
//            return null;
//        }
//    }
//
//    private static class Inner {
//        private static TTSUtil instance = new TTSUtil();
//    }
//    
//    public static void main(String[] args) throws InterruptedException {
//    	while (true) {
//    		Integer integer = integers.take();
//    		System.out.println(integer);
//		}
//	}
//}