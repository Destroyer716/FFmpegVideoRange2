package com.example.myplayer.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created By Ele
 * on 2020/5/30
 **/
public class KzgShaderUtil {

    /**
     * 读取gles文件，返回字符串
     * @param context
     * @param rawId
     * @return
     */
    public static String readRawTxt(Context context,int rawId){
        InputStream inputStream = context.getResources().openRawResource(rawId);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        String line;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
        }catch (Exception e ){
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 加载着色器
     * @param shaderType  着色器类型 有顶点着色器和片元着色器
     * @param source  着色器文件中的字符串
     * @return
     */
    public static int LoadShader(int shaderType,String source){
        //根据shader类型来创建shader 类型有顶点着色器，片元着色器
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0){
            //将着色器与源代码关联
            GLES20.glShaderSource(shader,source);
            //编译着色器
            GLES20.glCompileShader(shader);
            //检查是否编译成功
            int[] compile = new int[1];
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compile,0);
            if (compile[0] != GLES20.GL_TRUE){
                //如果失败了就删除着色器
                Log.e("kzg","创建着色器失败");
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }


    public static int createProgram(String vertexSource,String fragmentSource){
        //获取顶点着色器
        int vertexShader = LoadShader(GLES20.GL_VERTEX_SHADER,vertexSource);
        if (vertexShader == 0){
            return 0;
        }

        //获取片元着色器
        int fragmentShader = LoadShader(GLES20.GL_FRAGMENT_SHADER,fragmentSource);
        if (fragmentShader == 0){
            return 0;
        }

        //创建工程
        int program = GLES20.glCreateProgram();
        if (program != 0){
            GLES20.glAttachShader(program,vertexShader);
            GLES20.glAttachShader(program,fragmentShader);
            GLES20.glLinkProgram(program);
            int[] link = new int[1];
            GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,link,0);
            if (link[0] != GLES20.GL_TRUE){
                Log.e("kzg","创建工程失败");
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }
}
