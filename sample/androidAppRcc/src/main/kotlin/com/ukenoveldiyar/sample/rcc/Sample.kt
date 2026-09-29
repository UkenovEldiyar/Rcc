package com.ukenoveldiyar.sample.rcc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint
import com.ukenoveldiyar.sample.shared.RccColumn as Column
import com.ukenoveldiyar.sample.shared.RccRow as Row

@RccEntryPoint
@Composable
fun Sample() {
    val count = remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count.value.toString(),
            fontSize = 40.sp,
            style = TextStyle.Default,
        )

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    count.value += 1
                }
            ) {
                Text(
                    text = "+1",
                    fontSize = 40.sp,
                    style = TextStyle.Default
                )
            }

            Spacer(
                modifier = Modifier.width(width = 30.dp)
            )

            Button(
                onClick = {
                    count.value -= 1
                }
            ) {
                Text(
                    text = "-1",
                    fontSize = 40.sp,
                    style = TextStyle.Default
                )
            }
        }
    }
}
