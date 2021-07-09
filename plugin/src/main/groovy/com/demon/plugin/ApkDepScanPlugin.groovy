package com.demon.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

//https://www.jianshu.com/p/03c5886c2af9
class ApkDepScanPlugin implements Plugin<Project> {


    private final static String SCAN_DEPENDENCES = "scanDependences"
    private final static String PRINT_DEPENDENCES = "printDependences"
    private final static String PRINT_DEPENS = "printDepens"
    private final static String SAVE_PERMISSIONS = "save"


    @Override
    void apply(Project project) {
        if (!project.android) {
            throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!');
        }

        // 在编译成apk时执行
//        project.afterEvaluate {
//            def debugMergeTask = project.tasks.findByName('mergeDebugResources')
//            def releaseMergeTask = project.tasks.findByName('mergeReleaseResources')
//            if (releaseMergeTask != null) {
//                releaseMergeTask.doFirst {
//
//                }
//            }
//        }

        project.task(SAVE_PERMISSIONS) {
            doFirst {
                String saveFilePath = project.buildDir.getAbsolutePath() + "/permission_collection.csv"
                if (!project.buildDir.exists()) {
                    project.buildDir.mkdir()
                }
                println('persmission file save path: ' + saveFilePath)
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(saveFilePath))
                project.extensions.getByName('android').applicationVariants.all { variant ->
                    def name = "${variant.name}CompileClasspath"
                    println(name + " dependencies")
                    Configuration configuration = project.configurations.getByName(name)
                    configuration.getIncoming().files.each { dependency ->
                        if (dependency.getName().endsWith(".aar")) {
                            println("dependency: " + dependency.getAbsolutePath())
                            ZipFile zipFile = new ZipFile(dependency)
                            Enumeration<?> entries = zipFile.entries()
                            String folder = System.getProperty("java.io.tmpdir");
                            File cache = new File(folder, 'xmlcache')
                            if (!cache.isDirectory() || !cache.exists()) {
                                cache.mkdir()
                            }
                            while (entries.hasMoreElements()) {
                                ZipEntry entry = entries.nextElement()
                                if (entry.getName().equalsIgnoreCase('AndroidManifest.xml')) {
                                    File xmlFile = new File(cache, entry.getName())
                                    if (xmlFile.exists()) {
                                        xmlFile.delete()
                                    }
                                    InputStream is = zipFile.getInputStream(entry)
                                    FileOutputStream fos = new FileOutputStream(xmlFile)
                                    int len;
                                    byte[] buf = new byte[1024];
                                    while ((len = is.read(buf)) != -1) {
                                        fos.write(buf, 0, len);
                                        buf = new byte[1024]
                                    }
                                    // 关流顺序，先打开的后关闭
                                    fos.close()
                                    is.close()

                                    List<String> permissionList = new ArrayList<>();
                                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
                                    try {
                                        DocumentBuilder builder = factory.newDocumentBuilder();
                                        Document d = builder.parse(xmlFile);
                                        NodeList manifestNodeList = d.getElementsByTagName("manifest");
                                        for (int i = 0; i < manifestNodeList.getLength(); i++) {
                                            Node sonNode = manifestNodeList.item(i);
                                            NodeList grandSonNodeList = sonNode.getChildNodes();
                                            for (int j = 0; j < grandSonNodeList.getLength(); j++) {
                                                Node grandSonNode = grandSonNodeList.item(j);
                                                if (grandSonNode.getNodeType() == Node.ELEMENT_NODE) {
                                                    if (grandSonNode.getNodeName().toLowerCase().contains("uses-permission")) {
                                                        Element en = (Element) grandSonNode;
                                                        String value = en.getAttribute("android:name");
                                                        permissionList.add(value)
                                                        System.out.println(grandSonNode.getNodeName() + ": " + value);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        println('error msg: ' + e.getMessage())
                                    }

                                    //写入已经打开的文件
                                    if (permissionList.size() != 0) {
                                        bufferedWriter.write(dependency.getAbsolutePath())
                                        bufferedWriter.write(',')
                                        for (String permission : permissionList) {
                                            bufferedWriter.write(permission)
                                            bufferedWriter.write(',')
                                        }
                                        bufferedWriter.write('\n')
                                    }
                                    xmlFile.delete()
                                }
                            }
                        }
                    }
                }
                bufferedWriter.close()
            }
        }

        project.task(PRINT_DEPENS) {
            doLast {
                project.configurations.each { configuration ->
                    println("configuration: " + configuration.getName())
                    if (configuration.isCanBeResolved() && !configuration.getName().toLowerCase().contains('debug') &&
                            !configuration.getName().toLowerCase().contains('test')) {
                        configuration.each { dependency ->
                            println(dependency)
                            if (dependency.getName().endsWith(".aar")) {
                                println("dependency: " + dependency.getAbsolutePath())
                                ZipFile zipFile = new ZipFile(dependency)
                                Enumeration<?> entries = zipFile.entries()
                                String folder = System.getProperty("java.io.tmpdir");
                                File cache = new File(folder, 'xmlcache')
                                if (!cache.isDirectory() || !cache.exists()) {
                                    cache.mkdir()
                                }
                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = entries.nextElement()
                                    if (entry.getName().equalsIgnoreCase('AndroidManifest.xml')) {
                                        File xmlFile = new File(cache, entry.getName())
                                        if (xmlFile.exists()) {
                                            xmlFile.delete()
                                        }
                                        InputStream is = zipFile.getInputStream(entry)
                                        FileOutputStream fos = new FileOutputStream(xmlFile)
                                        int len;
                                        byte[] buf = new byte[1024];
                                        while ((len = is.read(buf)) != -1) {
                                            fos.write(buf, 0, len);
                                            buf = new byte[1024]
                                        }
                                        // 关流顺序，先打开的后关闭
                                        fos.close()
                                        is.close()

                                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
                                        try {
                                            DocumentBuilder builder = factory.newDocumentBuilder()
                                            Document d = builder.parse(xmlFile)
                                            NodeList sList = d.getElementsByTagName("uses-permission")
                                            for (int i = 0; i < sList.getLength(); i++) {
                                                Node node = sList.item(i)
                                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                                    Element en = (Element) node
                                                    String value = en.getAttribute("android:name")
                                                    println("uses-permission: " + value)
                                                }
                                            }
                                        } catch (Exception e) {
                                            println('error msg: ' + e.getMessage())
                                        }

                                        xmlFile.delete()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        project.task(PRINT_DEPENDENCES) {
            doLast {
                project.configurations.getByName('releaseCompileClasspath').each { dependency ->
                    println(dependency)
                    ZipFile zipFile = new ZipFile(dependency)
                    Enumeration<?> entries = zipFile.entries()
                    String folder = System.getProperty("java.io.tmpdir");
                    File cache = new File(folder, 'xmlcache')
                    if (!cache.isDirectory() || !cache.exists()) {
                        cache.mkdir()
                    }
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement()
                        if (entry.getName().equalsIgnoreCase('AndroidManifest.xml')) {
                            File xmlFile = new File(cache, entry.getName())
                            if (xmlFile.exists()) {
                                xmlFile.delete()
                            }
                            InputStream is = zipFile.getInputStream(entry)
                            FileOutputStream fos = new FileOutputStream(xmlFile)
                            int len;
                            byte[] buf = new byte[1024];
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                buf = new byte[1024]
                            }
                            // 关流顺序，先打开的后关闭
                            fos.close()
                            is.close()

                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
                            try {
                                DocumentBuilder builder = factory.newDocumentBuilder()
                                Document d = builder.parse(xmlFile)
                                NodeList sList = d.getElementsByTagName("uses-permission")
                                for (int i = 0; i < sList.getLength(); i++) {
                                    Node node = sList.item(i)
                                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                                        Element en = (Element) node
                                        String value = en.getAttribute("android:name")
                                        println("uses-permission: " + value)
                                    }
                                }
                            } catch (Exception e) {
                                println('error msg: ' + e.getMessage())
                            }
                        }
                    }
                }
            }
        }


        project.task(SCAN_DEPENDENCES) {
            doLast {
                println(SCAN_DEPENDENCES + " start ...")
                Configuration configuration = project.configurations.getByName('releaseCompileClasspath')
                final DependencySet dependencies = configuration.getIncoming().getDependencies()
                dependencies.each { Dependency dependency ->
                    println('dependency: ' + dependency)
                }
                final ArtifactCollection artifacts = configuration.getIncoming().getArtifacts()
                artifacts.each { artifact ->
                    println('artifact: ' + artifact)
                }

                final Set<? extends DependencyResult> allDependencies = configuration.getIncoming().getResolutionResult().getAllDependencies();
                allDependencies.each { dependencyResult ->
                    println('dependencyResult: ' + dependencyResult)
                    // 这个依赖为什么被引入。可能是一个project，也可能是另一个依赖
                    final ResolvedComponentResult from = dependencyResult.getFrom();
                    println('from: ' + from)

                    // 这个依赖被引入的时候是怎么依赖。
                    // ModuleComponentSelector: 使用module依赖， compile "xxx:xxx:1.0"
                    // ProjectComponentSelector: 使用project方式依赖, compile project(':xx')
                    // LibraryComponentSelector：不知道了
                    final ComponentSelector requested = dependencyResult.getRequested();
                    String groupStr;
                    String moduleStr;
                    String versionStr;
                    if (requested instanceof ModuleComponentSelector) {
                        final ModuleComponentSelector module = (ModuleComponentSelector) requested;
                        groupStr = module.getGroup();
                        moduleStr = module.getModule();
                        versionStr = module.getVersion();
                    }
                    // 解析成功的依赖
                    if (dependencyResult instanceof ResolvedDependencyResult) {
                        final ResolvedComponentResult selected = ((ResolvedDependencyResult) dependencyResult).getSelected();
                        // 如果是 compile "xx:xx:1.+"这种方式依赖
                        // 这里就可以得到最终版本号
                        versionStr = selected.getModuleVersion().getVersion();
                    } else {// 解析失败的依赖
                        final UnresolvedDependencyResult failed = (UnresolvedDependencyResult) dependencyResult;
                        // 在哪个几个maven仓库找过
                        final ComponentSelector attempted = failed.getAttempted();
                        final ComponentSelectionReason attemptedReason = failed.getAttemptedReason();
                        // 找不到的原因
                        final Throwable failure = failed.getFailure();
                    }
//                    println(groupStr + ':' + moduleStr + ':' + versionStr)
                }
            }
        }
    }
}