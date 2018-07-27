package com.cmiot.sip_demo.test;

import java.io.IOException;

import java.util.Vector;

 

import javax.media.CaptureDeviceInfo;

import javax.media.CaptureDeviceManager;

import javax.media.DataSink;

import javax.media.Manager;

import javax.media.MediaLocator;

import javax.media.NoDataSinkException;

import javax.media.NoProcessorException;

import javax.media.Processor;

import javax.media.control.StreamWriterControl;

import javax.media.format.AudioFormat;

import javax.media.protocol.DataSource;

import javax.media.protocol.FileTypeDescriptor;

 

public class CaptureAudio {

      /**

       * Writing captured audio to a file with a DataSink.

       */

      public static void main(String[] args) {

           CaptureDeviceInfo di = null;

          Processor p = null;

          StateHelper sh = null;

 //查询CaptureDeviceManager，来定位你需要使用的媒体采集设备。

          Vector deviceList = CaptureDeviceManager.getDeviceList(new

                           AudioFormat(AudioFormat.LINEAR, 44100, 16, 2));

            if (deviceList.size() > 0){

//得到此设备的CaptureDeviceInfo实例。

                di = (CaptureDeviceInfo)deviceList.firstElement();

                }

            else

                // 找不到满足（linear, 44100Hz, 16 bit,stereo audio.）音频设备，退出。

                System.exit(-1);

            try {

             //获得MediaLocator，并由此创建一个Processor。

                  p = Manager.createProcessor(di.getLocator());

                sh = new StateHelper(p);

             } catch (IOException e) {

                  e.printStackTrace();

                System.exit(-1);

            } catch (NoProcessorException e) {

                  e.printStackTrace();

                System.exit(-1);

            }

            // Configure the processor

            if (!sh.configure(10000)){

                  System.out.println("configure wrong!");

                System.exit(-1);

                }

            //定义待存储该媒体的内容类型（content type）。

            p.setContentDescriptor(new

                        FileTypeDescriptor(FileTypeDescriptor.WAVE));

         // realize the processor.

            if (!sh.realize(10000)){

                  System.out.println("realize wrong!");

                System.exit(-1);

                }

            // get the output of the processor

           DataSource source = p.getDataOutput();

         //定义存储该媒体的文件。

           MediaLocator dest = new MediaLocator(new java.lang.String(

                 "file:///D:/foo.wav"));

         //创建一个数据池

            DataSink filewriter = null;

            try {

                filewriter = Manager.createDataSink(source, dest);

                filewriter.open();

            } catch (NoDataSinkException e) {

                  e.printStackTrace();

                System.exit(-1);

            } catch (IOException e) {

                  e.printStackTrace();

                System.exit(-1);

            } catch (SecurityException e) {

                  e.printStackTrace();

                System.exit(-1);

            }

            // if the Processor implements StreamWriterControl, we can

            // call setStreamSizeLimit

            // to set a limit on the size of the file that is written.

            StreamWriterControl swc = (StreamWriterControl)

                p.getControl("javax.media.control.StreamWriterControl");

            //set limit to 5MB

            if (swc != null)

                swc.setStreamSizeLimit(5000000);

            // now start the filewriter and processor

            try {

                filewriter.start();

            } catch (IOException e) {

                  e.printStackTrace();

                System.exit(-1);

            }

            // Capture for 5 seconds

            sh.playToEndOfMedia(5000);

            sh.close();

            // Wait for an EndOfStream from the DataSink and close it...

            filewriter.close();

      }

}