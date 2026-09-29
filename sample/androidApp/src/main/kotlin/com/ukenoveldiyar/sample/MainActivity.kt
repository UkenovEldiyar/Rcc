package com.ukenoveldiyar.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ukenoveldiyar.rcc.runtime.HostComponent
import com.ukenoveldiyar.rcc.runtime.ModuleProgram
import com.ukenoveldiyar.rcc.runtime.ModuleProgramLoader
import com.ukenoveldiyar.rcc.runtime.Render
import com.ukenoveldiyar.rcc.runtime.hostComponent
import com.ukenoveldiyar.sample.shared.createAlignment
import com.ukenoveldiyar.sample.shared.createAny
import com.ukenoveldiyar.sample.shared.createArrangement
import com.ukenoveldiyar.sample.shared.createButton
import com.ukenoveldiyar.sample.shared.createButtonDefaults
import com.ukenoveldiyar.sample.shared.createClickable
import com.ukenoveldiyar.sample.shared.createColor
import com.ukenoveldiyar.sample.shared.createColumn
import com.ukenoveldiyar.sample.shared.createComposableLambda
import com.ukenoveldiyar.sample.shared.createComposables
import com.ukenoveldiyar.sample.shared.createComposer
import com.ukenoveldiyar.sample.shared.createDp
import com.ukenoveldiyar.sample.shared.createJavaLangString
import com.ukenoveldiyar.sample.shared.createModifier
import com.ukenoveldiyar.sample.shared.createRow
import com.ukenoveldiyar.sample.shared.createSnapshotState
import com.ukenoveldiyar.sample.shared.createSpacer
import com.ukenoveldiyar.sample.shared.createText
import com.ukenoveldiyar.sample.shared.createTextOverflow
import com.ukenoveldiyar.sample.shared.createTextStyle
import com.ukenoveldiyar.sample.shared.createTextUnit

private const val TAG = "RccVerify"

private const val SAMPLE_FQNAME = "com.ukenoveldiyar.sample.rcc.Sample"

private class AssetModuleProgramLoader(private val bytes: ByteArray) : ModuleProgramLoader() {
    override fun findModule(name: String) = defineModule(name, bytes)
}

private fun realHostComponent(): HostComponent = hostComponent(20) {
    module(createAny())
    module(createColumn())
    module(createRow())
    module(createSnapshotState())
    module(createModifier())
    module(createClickable())
    module(createComposables())
    module(createComposer())
    module(createComposableLambda())
    module(createColor())
    module(createTextUnit())
    module(createTextOverflow())
    module(createTextStyle())
    module(createText())
    module(createArrangement())
    module(createAlignment())
    module(createButton())
    module(createButtonDefaults())
    module(createJavaLangString())
    module(createDp())
    module(createSpacer())
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bytes = assets.open("sample.rcc").use { it.readBytes() }
        val program = AssetModuleProgramLoader(bytes).findModule("sample")
        val component = realHostComponent()

        setContent {
            Scaffold { padding ->
                RccHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    program = program,
                    component = component,
                )
            }
        }
    }
}

@Composable
private fun RccHost(
    modifier: Modifier,
    program: ModuleProgram,
    component: HostComponent,
) {
    Box(
        modifier = modifier
    ) {
        Render(
            fqName = SAMPLE_FQNAME,
            program = program,
            component = component,
        )
    }
}
