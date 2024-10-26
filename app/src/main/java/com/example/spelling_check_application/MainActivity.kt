package com.example.spelling_check_application

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.spelling_check_application.databinding.ActivityMainBinding
import com.example.spelling_check_application.network.NetworkService
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 현재 작업 중인 파일의 이름을 저장하는 변수
    private var currentFileName: String? = null

    // 내용 변경 여부를 확인하는 변수
    private var isContentChanged = false

    // 수정된 텍스트를 저장할 변수
    private var correctedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 설정
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뷰 요소들 가져오기
        val btnSend = findViewById<Button>(R.id.btn_send) // 보내기 버튼
        val btnSave = findViewById<Button>(R.id.btn_save) // 저장 버튼
        val btnCheck = findViewById<Button>(R.id.btn_check) // 검사하기 버튼
        val btnApply = findViewById<Button>(R.id.btn_apply) // 적용하기 버튼
        val btnCancel = findViewById<Button>(R.id.btn_cancel) // 취소하기 버튼
        val editTextWrite = findViewById<EditText>(R.id.editText_text_Write) // 작성 화면의 에디트 텍스트
        val editTextBodyText = findViewById<EditText>(R.id.editText_bodyText) // 본문 화면의 에디트 텍스트

        // 인텐트로부터 파일 이름 받기
        val filename = intent.getStringExtra("FILE_NAME")
        if (filename != null) {
            currentFileName = filename // 파일 이름 저장
            input(currentFileName!!) // 파일 내용 읽어오기
            // 액션 바에 파일 이름 표시
            supportActionBar?.title = currentFileName
        }

        // 에디트 텍스트에 TextWatcher 추가하여 내용 변경 감지
        editTextBodyText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isContentChanged = true // 내용이 변경되었음을 표시
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 글자수 세기 함수 호출
        textCounter()

        // 하단 네비게이션 설정
        setBottomNavigation()

        // 보내기 버튼 클릭 리스너
        btnSend.setOnClickListener {
            textSend()
        }

        // 취소 버튼 클릭 리스너
        btnCancel.setOnClickListener(){
            textCheckCancel()
        }

        // 세이브 버튼 클릭 리스너
        btnSave.setOnClickListener {
            if (currentFileName != null) {
                output()
                isContentChanged = false
                Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "저장할 파일 이름이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        //검사하기 버튼 클릭 리스너
        btnCheck.setOnClickListener {
            val inputText = editTextWrite.text.toString().trim()

            if (inputText.isNotEmpty()) {
                val networkService = NetworkService(BuildConfig.API_URL)
                networkService.sendTextToServer(inputText) { response ->
                    runOnUiThread {
                        if (response != null) {
                            correctedText = response.corrected_text // 수정된 텍스트 저장
                            val spellingCorrections = response.spelling_corrections

                            // 결과를 화면에 표시
                            textCheck("수정된 텍스트\n-------------\n\n$correctedText\n\n\n교정\n-------------\n\n$spellingCorrections")
                            textApplyVisibility()
                        } else {
                            Toast.makeText(this, "서버 요청 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "텍스트를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        //적용하기 버튼 클릭 리스너
        btnApply.setOnClickListener {
            if (!correctedText.isNullOrEmpty()) {
                editTextWrite.setText(correctedText) // 수정된 전체 텍스트 적용
                textApplyVisibilityReturn()
                textCheckReturn()
                Toast.makeText(this, "적용에 성공했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "적용할 수정된 텍스트가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 글자수 세는 함수
    private fun textCounter() {
        val editTextWrite = findViewById<EditText>(R.id.editText_text_Write)
        val textCount = findViewById<TextView>(R.id.letterCount_textview)
        val editTextBodyText = findViewById<EditText>(R.id.editText_bodyText)
        val bodyTextCount = findViewById<TextView>(R.id.bodyText_letterCount_textview)

        editTextWrite.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textCount.text = "글자수 : ${editTextWrite.text.length}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editTextBodyText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bodyTextCount.text = "글자수 : ${editTextBodyText.text.length}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // 하단 네비게이션 설정 함수
    private fun setBottomNavigation() {
        binding.BottomNavigationView.selectedItemId = R.id.navigation_write

        binding.BottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_write -> {
                    onClickWrite()
                    true
                }
                R.id.navigation_bodytext -> {
                    onClickBodytext()
                    true
                }
                else -> {
                    if (isContentChanged) {
                        onSaveMessage()
                        false // 다이얼로그 처리 후 네비게이션 선택을 취소
                    } else {
                        onClickSavelist()
                        true
                    }
                }
            }
        }
    }

    // 작성 화면으로 전환
    private fun onClickWrite(): Boolean {
        val writeLayout = findViewById<LinearLayout>(R.id.Write_Layout)
        val bodyTextLayout = findViewById<LinearLayout>(R.id.bodyText_Layout)

        val writeLetterCount = findViewById<TextView>(R.id.letterCount_textview)
        val bodytextLetterCount = findViewById<TextView>(R.id.bodyText_letterCount_textview)


        writeLayout.visibility = View.VISIBLE
        bodyTextLayout.visibility = View.INVISIBLE

        writeLetterCount.visibility = View.VISIBLE
        bodytextLetterCount.visibility = View.GONE

        val btnSend = findViewById<Button>(R.id.btn_send)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        btnSend.visibility = View.VISIBLE
        btnSave.visibility = View.INVISIBLE
        btnCheck.visibility = View.VISIBLE
        btnApply.visibility = View.GONE

        return true
    }

    // 본문 화면으로 전환
    private fun onClickBodytext(): Boolean {
        val writeLayout = findViewById<LinearLayout>(R.id.Write_Layout)
        val bodyTextLayout = findViewById<LinearLayout>(R.id.bodyText_Layout)
        val writeLetterCount = findViewById<TextView>(R.id.letterCount_textview)
        val bodytextLetterCount = findViewById<TextView>(R.id.bodyText_letterCount_textview)

        writeLayout.visibility = View.INVISIBLE
        bodyTextLayout.visibility = View.VISIBLE

        writeLetterCount.visibility = View.GONE
        bodytextLetterCount.visibility = View.VISIBLE

        val btnSend = findViewById<Button>(R.id.btn_send)
        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnApply = findViewById<Button>(R.id.btn_apply)

        btnSend.visibility = View.INVISIBLE
        btnSave.visibility = View.VISIBLE
        btnCheck.visibility = View.GONE
        btnApply.visibility = View.GONE

        return true
    }

    // 저장소 화면으로 전환
    private fun onClickSavelist(): Boolean {
        //val intent = Intent(this, TextListSaveActivity::class.java)
        //startActivity(intent)
        finish() //액티비티가 쌓이는 것을 방지 하기 위해 액티비티 종료
        return true
    }

    // 텍스트 보내기 함수
    private fun textSend() {
        val editTextWrite = findViewById<EditText>(R.id.editText_text_Write)
        val editTextBodyText = findViewById<EditText>(R.id.editText_bodyText)

        if (editTextWrite.text.toString().isNotEmpty()) {
            Toast.makeText(this@MainActivity, "작성하신 글을 본문으로 보냈습니다.", Toast.LENGTH_SHORT).show()

            if (editTextBodyText.text.toString().isEmpty()) {
                // 본문 칸이 비어 있을 경우 줄바꿈 없이 텍스트 추가
                editTextBodyText.setText("${editTextWrite.text}")
            } else {
                // 본문 칸에 내용이 있을 경우 두 개의 줄바꿈 후 텍스트 추가
                editTextBodyText.append("\n\n${editTextWrite.text}")
            }
            editTextWrite.text = null

            isContentChanged = true
        } else {
            Toast.makeText(this@MainActivity, "작성할 텍스트가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 텍스트 검사 결과 표시 함수
    private fun textCheck(textChecker: String) {
        val textCheck = findViewById<TextView>(R.id.textcheck)

        textCheck.visibility = View.VISIBLE
        textCheck.text = textChecker
    }

    // 텍스트 검사 결과 숨기기
    private fun textCheckReturn() {
        val textCheck = findViewById<TextView>(R.id.textcheck)
        textCheck.visibility = View.GONE
    }

    // 취소 버튼
    private fun textCheckCancel(){
        textCheckReturn()
        textApplyVisibilityReturn()
        Toast.makeText(this, "취소합니다.", Toast.LENGTH_SHORT).show()
    }

    // 적용하기, 취소 버튼 보이기
    private fun textApplyVisibility() {
        val btnApply = findViewById<Button>(R.id.btn_apply)
        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnCansel = findViewById<Button>(R.id.btn_cancel)
        val btnSend = findViewById<Button>(R.id.btn_send)

        btnApply.visibility = View.VISIBLE
        btnCheck.visibility = View.GONE
        btnCansel.visibility = View.VISIBLE
        btnSend.visibility = View.GONE
    }

    // 적용하기, 취소 버튼 숨기기
    private fun textApplyVisibilityReturn() {
        val btnApply = findViewById<Button>(R.id.btn_apply)
        val btnCheck = findViewById<Button>(R.id.btn_check)
        val btnCansel = findViewById<Button>(R.id.btn_cancel)
        val btnSend = findViewById<Button>(R.id.btn_send)

        btnApply.visibility = View.GONE
        btnCheck.visibility = View.VISIBLE
        btnCansel.visibility = View.GONE
        btnSend.visibility = View.VISIBLE
    }

    // 파일 저장 함수
    private fun output() {
        if (currentFileName == null) {
            Toast.makeText(this, "저장할 파일 이름이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 파일 이름에서 유효하지 않은 문자 제거 및 확장자 추가
        val sanitizedFileName = if (currentFileName!!.endsWith(".txt")) {
            currentFileName!!.replace(Regex("[\\\\/:*?\"<>|]"), "")
        } else {
            "${currentFileName!!.replace(Regex("[\\\\/:*?\"<>|]"), "")}.txt"
        }

        val filePath = File(filesDir, sanitizedFileName)
        val textContent = findViewById<EditText>(R.id.editText_bodyText).text.toString()

        try {
            val fileWriter = FileWriter(filePath)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(textContent)
            bufferedWriter.close()

            isContentChanged = false // 저장 후 변경 사항 없음으로 설정

            Toast.makeText(this, "파일이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "파일 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    // 파일 읽기 함수
    private fun input(filename: String) {
        // 파일 이름에서 유효하지 않은 문자 제거 및 확장자 추가
        val sanitizedFileName = if (filename.endsWith(".txt")) {
            filename.replace(Regex("[\\\\/:*?\"<>|]"), "")
        } else {
            "${filename.replace(Regex("[\\\\/:*?\"<>|]"), "")}.txt"
        }

        val filePath = File(filesDir, sanitizedFileName)

        if (filePath.exists()) {
            try {
                val fileReader = FileReader(filePath)
                val bufferedReader = BufferedReader(fileReader)
                val stringBuilder = StringBuilder()
                var line: String?

                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                    stringBuilder.append('\n')
                }

                bufferedReader.close()

                val content = stringBuilder.toString()
                // 읽은 내용을 에디트 텍스트에 표시
                val textView = findViewById<EditText>(R.id.editText_bodyText)
                textView.setText(content)

                // 글자 수 업데이트 코드 추가
                val bodyTextCount = findViewById<TextView>(R.id.bodyText_letterCount_textview)
                bodyTextCount.text = "글자수 : ${content.length}"

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "파일 읽기 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }




    // 저장 여부를 묻는 다이얼로그 표시
    private fun onSaveMessage() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("저장하셨습니까?")
        dialog.setMessage("저장하지 않은 부분은 사라집니다.")

        dialog.setPositiveButton("확인") { dialog, _ ->
            // 저장하지 않고 저장소로 이동
            Toast.makeText(this, "저장하지 않고 저장소로 이동합니다.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            // 저장소로 이동
            onClickSavelist()
        }
        dialog.setNegativeButton("취소") { dialog, _ ->
            // 다이얼로그 닫기
            dialog.dismiss()
        }
        dialog.show()
    }
}