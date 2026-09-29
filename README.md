# Remote Code Constructor

RCC is a proof of concept of a Kotlin interpreter with first-class support for the Kotlin object 
model and the ability to call native Kotlin functions directly. On top of it, Server-Driven UI (SDUI) 
becomes possible without a custom JSON schema or DSL: you write ordinary Kotlin code, compile it 
into RCC bytecode, deliver it to the device, and the runtime renders the UI.

Kotlin code -> RCC compiler -> RCC bytecode (.rcc) -> RCC runtime
c
## Why

Most SDUI solutions describe screens with JSON or a custom DSL. That means a second "language" with 
its own tooling, weak typing, no IDE support, and constant mapping between schema and native components.

RCC explores a different approach: the UI description is regular Kotlin. It is type-checked by 
the Kotlin compiler, supported by the IDE, and can call existing Kotlin code in the host app.

## Demo

sample/androidApp contains sample.rcc, a binary file with opcode instructions generated from the 
regular Kotlin code in Sample.kt. 
The Android app loads this file and executes it with the RCC runtime.

https://github.com/user-attachments/assets/35204ab8-ec66-4899-b306-6e0672684b94

## Status

Proof of concept. The end-to-end pipeline works on the sample, but the project is not 
production-ready and is not under consideration. 

I'm reading now book: Compilers: Principles, Techniques, and Tools

### Lessons lessons and working on mistakes:

Building RCC showed that a system like this needs a coherent language design from the start,
not just a working runtime. The compiler backend turned out to be the hardest part. It requires
a deep understanding of compiler design. I made the mistake of doing this part with Claude, in the 
end I stopped understanding, and just wanted to get a working concept.

So I'm pausing the project to study the theory and plan to return to RCC with a proper design.
