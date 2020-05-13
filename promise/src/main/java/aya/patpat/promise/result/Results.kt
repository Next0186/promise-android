package aya.patpat.promise.result

class Results {

    companion object {
        const val SUCCESS = "SUCCESS"
        const val FAILURE = "FAILURE"
        const val CANCEL = "CANCEL"
        const val ERR_TIMEOUT = "ERR_TIMEOUT"
        const val ERR_INTERNAL = "ERR_INTERNAL"
        const val ERR_SERVER = "ERR_SERVER"
        const val ERR_PARAMS_INVALID = "ERR_PARAMS_INVALID"
        const val ERR_NETWORK = "ERR_NETWORK"
    }

    open class Normal<T>(val data: T? = null, result: String = "", msg: String = "") : BaseResult(result, msg)
    class Success<T>(data: T? = null, msg: String = "操作成功") : Normal<T>(data, SUCCESS, msg)
    class Failure(msg: String = "操作失败") : BaseResult(FAILURE, msg)
    class Cancel(msg: String = "取消") : BaseResult(CANCEL, msg)
    class Timeout(msg: String = "操作超时") : BaseResult(ERR_TIMEOUT, msg)
    class ErrInternal(msg: String = "内部错误") : BaseResult(ERR_INTERNAL, msg)
    class ErrServer(msg: String = "服务器故障") : BaseResult(ERR_SERVER, msg)
    class ErrParamsInvalid(msg: String = "参数无效") : BaseResult(ERR_PARAMS_INVALID, msg)
    class ErrNetwork(msg: String = "网络错误") : BaseResult(ERR_NETWORK, msg)
}
