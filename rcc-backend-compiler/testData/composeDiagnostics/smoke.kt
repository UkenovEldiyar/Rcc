// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 11
// BOUND_FUNCTION: androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint

@RccEntryPoint
@Composable
fun test() {
    Column {

    }
}
