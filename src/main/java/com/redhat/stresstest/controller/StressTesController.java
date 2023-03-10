package com.redhat.stresstest.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;

import com.redhat.stresstest.model.StressTestCase;

import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping(value = "/api/stresstest")
public class StressTesController {

    private static Logger log = LoggerFactory.getLogger(StressTesController.class);

    class Greeting {
        public String getGreeting() {
            return "Greetings from Spring Boot!";
        }
    }

    class StressTestStartResult {
        public String getStressTestStartResult() {
            return "Teste Iniciado!";
        }
    }

    @ResponseBody
    @GetMapping()
    public Greeting index() throws InterruptedException, ExecutionException {

        log.info("Teste assincrono 1");
        asyncMethodWithReturnType1();
        log.info("Teste assincrono 2");
        return new Greeting();
    }

    private static final String EXTENSION = ".zip";

    @RequestMapping(path = "/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam("report") String report) throws IOException {
        
        File file = new File("/home/jboss/app/reports"+ File.separator + report + EXTENSION);
        //File file = new File("reports"+ File.separator + report + EXTENSION);

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+report+EXTENSION);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }

    @ResponseBody
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StressTestStartResult startTest(@RequestBody StressTestCase stressTestCase)
            throws InterruptedException, ExecutionException, IOException {

        log.info("start rest method startTest");
        startTestAsync(stressTestCase);
        log.info("end method startTest");
        return new StressTestStartResult();
    }

    @Async("asyncExecutor")
    public CompletableFuture<String> startTestAsync(StressTestCase stressTestCase)
            throws InterruptedException, IOException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {

            log.info("init test name:" + stressTestCase.getName());

            //String homeDirectory = System.getProperty("user.home");
            String homeDirectory = System.getProperty("user.home")+"/app";

            log.info("homeDirectory:" + homeDirectory);
            
            //execCmd(String.format("mvn gatling:test", homeDirectory));
            String[] cmd = { "/bin/bash", "-c", "cd /home/jboss/app; mvn gatling:test" };
            execCmd(cmd);

            //execCmd(String.format("cp -r ./target/gatling ./reports", homeDirectory));
            String[] cmd1 = { "/bin/bash", "-c", "cp -r /home/jboss/app/target/gatling /home/jboss/app/reports" };
            execCmd(cmd1);

            //zipFolderTest("./reports/gatling", "./reports/"+stressTestCase.getName());
            zipFolderTest("/home/jboss/app/reports/gatling", "/home/jboss/app/reports/"+stressTestCase.getName());

            completableFuture.complete("Success");
            log.info("end test name:" + stressTestCase.getName());

            return null;
        });

        return completableFuture;
    }

    public static void execCmd(String cmd) throws java.io.IOException {

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(cmd);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        // Read the output from the command
        log.info("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            log.info(s);
        }

        // Read any errors from the attempted command
        log.info("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            log.info(s);
        }
    }

    public static void execCmd(String[] command) throws java.io.IOException {

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(command);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        // Read the output from the command
        log.info("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            log.info(s);
        }

        // Read any errors from the attempted command
        log.info("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            log.info(s);
        }
    }

    @Async("asyncExecutor")
    public CompletableFuture<String> asyncMethodWithReturnType1() throws InterruptedException {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();

        Executors.newCachedThreadPool().submit(() -> {
            Thread.sleep(5000);
            completableFuture.complete("Hello");
            log.info("Teste assincrono 3");
            return null;
        });

        return completableFuture;
    }

    private static void zipFolderTest(String sourceFile, String fileName) throws IOException{
        
        FileOutputStream fos = new FileOutputStream(fileName+".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        File fileToZip = new File(sourceFile);
        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
    }
    
    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

}
