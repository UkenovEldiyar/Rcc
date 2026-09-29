package com.ukenoveldiyar.rcc.compiler.plugin.runners;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class TempDiagnosticDump {
  @Test
  public void dump() {
    JvmDiagnosticTestGenerated t = new JvmDiagnosticTestGenerated();
    try {

      System.out.println("NO MISMATCH");
    } catch (AssertionFailedError e) {
      System.out.println("===ACTUAL-BEGIN===");
      System.out.println(e.getActual().getStringRepresentation());
      System.out.println("===ACTUAL-END===");
    }
  }
}
