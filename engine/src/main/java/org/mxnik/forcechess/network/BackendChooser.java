package org.mxnik.forcechess.network;

import org.nd4j.linalg.factory.Nd4j;

public class BackendChooser {

    public static void setGPU(){
        System.setProperty(
                "org.nd4j.linalg.factory.Nd4jBackend",
                "org.nd4j.linalg.jcublas.JCublasBackend"
        );
    }

    public static void setCPU(){
        System.setProperty(
                "org.nd4j.linalg.factory.Nd4jBackend",
                "org.nd4j.linalg.cpu.nativecpu.CpuBackend"
        );
    }

    public static void initDL4J(){
        Nd4j.create(1).close();
        System.out.println("Active Backend: " + Nd4j.getBackend().getClass().getName());
    }
}
