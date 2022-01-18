package com.jiangcm.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*

class FileUtils {


    companion object {

        private val parentPath = Environment.getExternalStorageDirectory()
        private var storagePath = ""
        private var DST_FOLDER_NAME = "JCamera"
        private var TAG = "FileJUtils"

        private fun initPath(): String {
            if (storagePath == "") {
                storagePath = parentPath.absolutePath + File.separator + DST_FOLDER_NAME
                createSavePath(storagePath)
            }
            return storagePath
        }

        fun saveBitmap(b: Bitmap?): Boolean {
            createSavePath(parentPath.absolutePath + File.separator)
            DST_FOLDER_NAME = "img"
            val path = initPath()
            val dataTake = System.currentTimeMillis()
            val name = "img_$dataTake.png"
            val jpegName = path + File.separator + name
            return try {
                val fos = FileOutputStream(jpegName)
                val bos = BufferedOutputStream(fos)
                b?.compress(Bitmap.CompressFormat.PNG, 100, bos)
                bos.flush()
                bos.close()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }


        /**
         * 判断传入的地址是否已经有这个文件夹，没有的话需要创建
         */
        fun createSavePath(path: String?) {
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
        }

        /**
         * 判断传入地址的文件是否存在
         */
        fun fileIsExists(strFile: String?): Boolean {
            try {
                val f = File(strFile)
                if (!f.exists()) {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
            return true
        }

        fun copyFile(context: Context?, fileName: String?, strOutFilePath: String?,strOutFileName: String?){
            if (!fileIsExists(strOutFileName)){
                createSavePath(strOutFilePath)
                copyFileToSD(context,fileName,strOutFileName)
            }
        }

        @Throws(IOException::class)
        fun copyFileToSD(context: Context?, fileName: String?, strOutFileName: String?) {
            val myOutput = FileOutputStream(strOutFileName)
            val myInput: InputStream? = context?.assets?.open(fileName?:"")
            val buffer = ByteArray(0x400)
            var length: Int? = myInput?.read(buffer)
            while (length?:0 > 0) {
                myOutput.write(buffer, 0x0, length?:0)
                length = myInput?.read(buffer)
            }
            myOutput.flush()
            myInput?.close()
            myOutput.close()
        }
    }






}