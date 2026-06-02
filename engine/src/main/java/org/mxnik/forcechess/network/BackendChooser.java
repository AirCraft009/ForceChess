package org.mxnik.forcechess.network;

import org.nd4j.linalg.factory.Nd4j;

public class BackendChooser {

    public void setGPU(){
        System.setProperty(
                "org.nd4j.linalg.factory.Nd4jBackend",
                "org.nd4j.linalg.jcublas.JCublasBackend"
        );
    }

    public void setCPU(){
        System.setProperty(
                "org.nd4j.linalg.factory.Nd4jBackend",
                "org.nd4j.linalg.cpu.nativecpu.CpuBackend"
        );
    }
}
