package com.backpackingmap.backpackingmap.net

data class Response<Error, Data>(val error: Error?, val data: Data?)
