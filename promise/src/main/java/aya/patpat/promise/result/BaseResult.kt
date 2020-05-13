package aya.patpat.promise.result

open class BaseResult(
    val result: String = "",
    val msg: String = ""
) {
    var id: Long = -1

    fun isSuccess(): Boolean {
        return "SUCCESS" == result
    }
}