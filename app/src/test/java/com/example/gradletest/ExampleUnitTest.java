package com.example.gradletest;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        File xmlFile = new File("C:\\Users\\alex\\AppData\\Local\\Temp\\xmlcache\\AndroidManifest.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document d = builder.parse(xmlFile);
            NodeList manifestNodeList = d.getElementsByTagName("manifest");
            if (manifestNodeList != null) {
                for (int i = 0; i < manifestNodeList.getLength(); i++) {
                    Node sonNode = manifestNodeList.item(i);
                    NodeList grandSonNodeList = sonNode.getChildNodes();
                    if (grandSonNodeList != null) {
                        for (int j = 0; j < grandSonNodeList.getLength(); j++) {
                            Node grandSonNode = grandSonNodeList.item(j);
                            if(grandSonNode != null){
                                if (grandSonNode.getNodeType() == Node.ELEMENT_NODE) {
                                    if (grandSonNode.getNodeName().toLowerCase().contains("uses-permission")) {
                                        Element en = (Element) grandSonNode;
                                        String value = en.getAttribute("android:name");
                                        System.out.println(grandSonNode.getNodeName() + ": " + value);
                                    }
                                }
                            }

                        }
                    }

                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

    }
}