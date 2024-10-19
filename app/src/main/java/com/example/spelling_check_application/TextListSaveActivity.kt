package com.example.spelling_check_application

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.io.File

class TextListSaveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_list_save)

        val btn_Test = findViewById<Button>(R.id.btn_test)
        btn_Test.setOnClickListener {
            onAlertDialog()
        }

        // 저장된 파일 목록 로드
        loadSavedFiles()
    }

    private fun addSave(saveName: String?) {
        val save_Scroll_Layout = findViewById<LinearLayout>(R.id.save_scroll_layout)
        val saveCardView = CardView(this)
        val layoutParamsbasic = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParamsbasic.setMargins(15, 15, 15, 15) // 카드뷰 간의 간격 조정

        saveCardView.layoutParams = layoutParamsbasic
        saveCardView.radius = 12F
        saveCardView.setContentPadding(25, 25, 25, 25)
        saveCardView.cardElevation = 8F

        val cardLinearLayout = LinearLayout(this)
        cardLinearLayout.orientation = LinearLayout.HORIZONTAL
        cardLinearLayout.gravity = Gravity.CENTER_VERTICAL

        val saveText = TextView(this)
        saveText.text = saveName
        saveText.textSize = 18f // 텍스트 크기 조정
        saveText.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )

        val btnDelete = ImageButton(this)
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete)
        btnDelete.background = null // 버튼 배경 제거
        btnDelete.setColorFilter(Color.parseColor("#FF6666")) // 부드러운 빨간색
        btnDelete.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val btnLayoutParams = btnDelete.layoutParams as LinearLayout.LayoutParams
        btnLayoutParams.setMargins(10, 0, 0, 0) // 버튼 마진 조정
        btnDelete.layoutParams = btnLayoutParams

        // 삭제 버튼 클릭 리스너 설정
        btnDelete.setOnClickListener {
            onDeleteFile(saveName, saveCardView)
        }

        if (saveText.length() != 0) {
            cardLinearLayout.addView(saveText)
            cardLinearLayout.addView(btnDelete)
            saveCardView.addView(cardLinearLayout)
            save_Scroll_Layout.addView(saveCardView)

            // 파일 항목 클릭 시 MainActivity로 이동
            saveCardView.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("FILE_NAME", saveName) // 파일 이름을 인텐트로 전달
                startActivity(intent)
            }
        }
    }

    private fun onDeleteFile(fileName: String?, cardView: CardView) {
        if (fileName == null) return

        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("삭제 확인")
        dialog.setMessage("파일 '$fileName'을(를) 삭제하시겠습니까?")

        dialog.setPositiveButton("삭제") { dialog, _ ->
            deleteFileFromStorage(fileName)
            val save_Scroll_Layout = findViewById<LinearLayout>(R.id.save_scroll_layout)
            save_Scroll_Layout.removeView(cardView)
            Toast.makeText(this, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteFileFromStorage(fileName: String) {
        val filePath = "${filesDir.absolutePath}/$fileName"
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun onAlertDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("제목을 작성해주세요.")

        val et = EditText(this)
        dialog.setView(et)

        dialog.setPositiveButton("확인") { dialog, _ ->
            val fileName = et.text.toString()
            if (fileName.isNotEmpty()) {
                // 파일 이름에서 유효하지 않은 문자 제거 및 확장자 추가
                val sanitizedFileName = if (fileName.endsWith(".txt")) {
                    fileName.replace(Regex("[\\\\/:*?\"<>|]"), "")
                } else {
                    "${fileName.replace(Regex("[\\\\/:*?\"<>|]"), "")}.txt"
                }
                val filePath = "${filesDir.absolutePath}/$sanitizedFileName"
                val file = File(filePath)
                if (file.exists()) {
                    Toast.makeText(this, "이미 존재하는 파일 이름입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    file.createNewFile()
                    Toast.makeText(this, "새로운 텍스트 파일 생성", Toast.LENGTH_SHORT).show()
                    addSave(sanitizedFileName)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("FILE_NAME", sanitizedFileName)
                    startActivity(intent)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "제목을 지어주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.setNeutralButton("취소") { dialog, _ ->
            Toast.makeText(this, "취소 합니다.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        dialog.show()
    }




    private fun loadSavedFiles() {
        val save_Scroll_Layout = findViewById<LinearLayout>(R.id.save_scroll_layout)
        save_Scroll_Layout.removeAllViews()

        val directory = filesDir
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    val filename = file.name
                    if (filename.endsWith(".txt")) {
                        addSave(filename)
                    }
                }
            }
        }
    }


}
