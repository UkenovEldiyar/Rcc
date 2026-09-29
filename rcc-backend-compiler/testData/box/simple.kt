package foo.bar

fun box1(
    value: Boolean
): String {
    if (value) {
        box3()
    }
    return box2().toString()
}

fun box2(): Int {
    return 0
}

fun box3() {

}