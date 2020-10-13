package com.streamr.client.dataunion;


import com.streamr.client.dataunion.contracts.DataUnionMainnet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.Address;
import org.web3j.codegen.SolidityFunctionWrapperGenerator;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JavaWrapperUtil {
    private static final Logger log = LoggerFactory.getLogger(JavaWrapperUtil.class);

    /*
     have to subclass SolidityFunctionWrapperGenerator because they made the constructor protected
     bad design!
     */
    public static class Gen extends SolidityFunctionWrapperGenerator {
        public Gen(File binFile, File abiFile, File destinationDir, String contractName, String basePackageName, boolean useJavaNativeTypes, boolean useJavaPrimitiveTypes, int addressLength) {
            super(binFile, abiFile, destinationDir, contractName, basePackageName, useJavaNativeTypes, useJavaPrimitiveTypes, addressLength);
        }
    }

    public static void makeJavaClass(Reader jsonfile, String contractName) throws IOException, ParseException, ClassNotFoundException {
        log.info("Making Java wrapper from Solidity contract " + contractName);
        // write .json to .bin and .abi Files.
        // cant use Reader... more bad web3j design!
        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(jsonfile);
        File binFile = File.createTempFile("JavaWrapperUtil", ".json");
        File abiFile = File.createTempFile("JavaWrapperUtil", ".json");
        FileWriter fw = new FileWriter(binFile);
        fw.write((String) root.get("bytecode"));
        fw.flush();
        fw.close();
        fw = new FileWriter(abiFile);
        fw.write(((JSONArray) root.get("abi")).toJSONString());
        fw.flush();
        fw.close();
        Gen gen = new Gen(
                binFile,
                abiFile,
                new File("src/main/java"),
                contractName,
                "com.streamr.client.dataunion.contracts",
                false,
                false,
                20
        );
        gen.generate();
        binFile.delete();
        abiFile.delete();

    }

    public static int wrapJsonInDir(File dir, FileFilter filter) throws IOException, ParseException, ClassNotFoundException {
        int wrapped = 0;
        for(File f : dir.listFiles(filter)) {
            String fname = f.getName();
            int len = fname.length();
            log.info("Processing " + fname);
            makeJavaClass(new FileReader(f), fname.substring(0, len - 5));
            wrapped++;
        }
        return wrapped;
    }

    public static void main(String[] args){
        String buildDir = args[0];
        //String buildDir = "data-union-solidity/build/contracts";
        log.info("Processing buildDir " + buildDir);
        try {
            wrapJsonInDir(new File(buildDir),
                    new FileFilter(){
                        @Override
                        public boolean accept(File file) {
                            String fname = file.getName();
                            return fname.endsWith(".json")  && fname.startsWith("DataUnion");
                        }
                    }
            );
        }
        catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

}
