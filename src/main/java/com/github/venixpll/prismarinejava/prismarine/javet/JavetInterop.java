package com.github.venixpll.prismarinejava.prismarine.javet;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Locker;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class JavetInterop {

    private final NodeRuntime nodeRuntime;
    private final Executor javetExecutor;
    private final ScheduledExecutorService tickTask;

    @Getter
    private boolean initialized;

    public JavetInterop(final File prismScript) throws JavetException, InterruptedException {
        final var latch = new CountDownLatch(1);

        this.nodeRuntime = V8Host.getNodeInstance().createV8Runtime();
        this.nodeRuntime.allowEval(true);
        this.javetExecutor = Executors.newSingleThreadExecutor();
        this.initializeInterop(prismScript,latch);

        // KeepAlive
        this.tickTask = Executors.newSingleThreadScheduledExecutor();
        this.startTicking();

        latch.await();
    }

    private void initializeInterop(final File prismScript, final CountDownLatch latch){
        this.javetExecutor.execute(() -> {

            try {
                this.nodeRuntime.getExecutor(prismScript).executeVoid();
                this.initialized = true;
                latch.countDown();
                this.nodeRuntime.await();
            }catch(final Exception exception){
                exception.printStackTrace();
            }
        });
    }

    private void startTicking(){
        this.tickTask.scheduleAtFixedRate(() -> {
            if(this.nodeRuntime == null || !this.initialized) return;

        },0,250, TimeUnit.MILLISECONDS);
    }

    public byte[] serialize(final ConnectionState connectionState, final String version, final JSONObject object, final boolean isServer) throws Exception {
        if(this.nodeRuntime == null || !this.initialized) return null;

        try (final V8ValueFunction v8ValueFunction = this.nodeRuntime.getGlobalObject().get("convertObjectToBuffer")) {
            return v8ValueFunction.callObject(null, connectionState.getName(), version, object.toString(), isServer);
        }
    }

    public JSONObject deserialize(final ConnectionState connectionState,final String version,final byte[] packetBuffer,final boolean isServer) throws Exception{
        if(this.nodeRuntime == null || !this.initialized) return null;

        try (final V8ValueFunction v8ValueFunction2 = this.nodeRuntime.getGlobalObject().get("convertBufferToObject")) {
            final var outputJson = v8ValueFunction2.callString(null, connectionState.getName(), version, packetBuffer, isServer);
            return JSON.parseObject(outputJson);
        }
    }
}
