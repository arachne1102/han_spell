package com.example.spelling_check_application.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class NetworkService {
    private val client = OkHttpClient()

    // 데이터를 JSON 형식으로 변환할 데이터 클래스 정의
    data class TextData(val text: String)

    // 서버로 텍스트를 보내고 응답을 받는 함수
    fun sendTextToServer(text: String, callback: (SpellCheckResponse?) -> Unit) {
        // Gson을 사용하여 데이터를 JSON 형식으로 변환
        val gson = Gson()
        val jsonData = gson.toJson(TextData(text)) // 데이터 직렬화: {"text": "입력한 텍스트"}

        // JSON 데이터를 RequestBody로 변환
        val requestBody = jsonData.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // HTTP 요청 생성
        val request = Request.Builder()
            .url("https://e81f-58-225-155-178.ngrok-free.app/check_spelling")  // FastAPI 서버의 ngrok 주소
            .post(requestBody)
            .build()

        // 비동기 HTTP 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace() // 실패 시 로그 출력
                callback(null) // 실패 시 콜백 호출
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                try {
                    // JSON 응답을 Gson을 사용해 객체로 변환
                    val spellCheckResponse = gson.fromJson(responseData, SpellCheckResponse::class.java)
                    callback(spellCheckResponse) // 성공 시 응답 객체를 콜백으로 반환
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace() // JSON 파싱 오류 처리
                    callback(null) // 오류 시 null 반환
                }
            }
        })
    }
}

// 응답을 처리할 데이터 클래스 정의
data class SpellCheckResponse(
    val original_text: String,
    val corrected_text: String,
    val spelling_corrections: String
)
