package com.github.venixpll.prismarinejava.prismarine;

import com.alibaba.fastjson2.JSON;
import com.github.venixpll.prismarinejava.PrismJava;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.prismarine.javet.JavetInterop;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.Getter;

public class PrismInitializer {

    @Getter
    private static JavetInterop javetInterop;

    public PrismInitializer() throws Exception {
        // Load PrismScript
        final var prismScript = PrismJava.class.getResource("/prismCodec.js");
        if(Objects.isNull(prismScript)) throw new FileNotFoundException("./prismScript.js not found in resources!");

        final var temp = Files.createTempFile("prismCodec",".js");
        Files.write(temp, Files.readAllBytes(Paths.get(prismScript.toURI())));

        // Run Script
        javetInterop = new JavetInterop(temp.toFile());
        this.warmUp();
    }

    /**
     *  Javet and any other JS to Java interop engine
     *  Needs a warmup to perform at its peak.
     *  That's why we will force some deserialization and serialization at the beginning.
     */
    private void warmUp() throws Exception {
        final var warmUpDataFile = PrismJava.class.getResource("/warmup.json");
        if(Objects.isNull(warmUpDataFile)) throw new FileNotFoundException("./warmup.json not found in resources!");
        final var warmUpData = JSON.parseObject(warmUpDataFile);

        var warmUpTime = 0L;
        //Serialize
        {
            final var startTime = System.currentTimeMillis();
            final var arr = warmUpData.getJSONArray("serialize");
            for(int i=0;i<arr.size();i++){
                final var obj = arr.getJSONObject(i);
                final var buf = javetInterop.serialize(ConnectionState.PLAY,"1.18.2",obj,false);
                final var parsed = javetInterop.deserialize(ConnectionState.PLAY,"1.18.2",buf,true);
                assert parsed.equals(obj);
            }
            warmUpTime = System.currentTimeMillis() - startTime;
        }

        // Post WarmUpTest
        {
            final var startTime = System.currentTimeMillis();
            final var arr = warmUpData.getJSONArray("serialize");
            for(int i=0;i<arr.size();i++){
                final var obj = arr.getJSONObject(i);
                final var buf = javetInterop.serialize(ConnectionState.PLAY,"1.18.2",obj,false);
                final var parsed = javetInterop.deserialize(ConnectionState.PLAY,"1.18.2",buf,true);
                assert parsed.equals(obj);
            }
            final var endTime = System.currentTimeMillis() - startTime;
            assert endTime < (warmUpTime / 10) : "Warmup did nothing to speed up the execution!";
        }

        System.out.println("Javet warmed up!");

    }

}
