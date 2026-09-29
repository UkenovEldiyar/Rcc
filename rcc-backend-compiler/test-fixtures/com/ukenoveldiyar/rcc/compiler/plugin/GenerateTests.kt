package com.ukenoveldiyar.rcc.compiler.plugin

import com.ukenoveldiyar.rcc.compiler.plugin.runners.AbstractJvmBoxTest
import com.ukenoveldiyar.rcc.compiler.plugin.runners.AbstractJvmComposeBoxTest
import com.ukenoveldiyar.rcc.compiler.plugin.runners.AbstractJvmComposeDiagnosticTest
import com.ukenoveldiyar.rcc.compiler.plugin.runners.AbstractJvmDiagnosticTest
import com.ukenoveldiyar.rcc.compiler.plugin.runners.AbstractRccBoxTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testsRoot = args[0], testDataRoot = args[1]) {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }

            testClass<AbstractJvmComposeBoxTest> {
                model("composeBox")
            }

            testClass<AbstractJvmComposeDiagnosticTest> {
                model("composeDiagnostics")
            }

            testClass<AbstractRccBoxTest> {
                model("rccBox")
            }
        }
    }
}
