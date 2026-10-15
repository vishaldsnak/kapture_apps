package com.mazda.gms3.mdm.rmitool.utils;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class Test {

	public static void main(String[] args) 
	{
		try
		{
			String test="SI%20ABC.png";
			test = test.replace("%20", " ");
			System.out.println(test);
			
			File sourceDir = new File("D:\\RMITOOL_WD\\en_CA\\service_information\\tsb\\si123456");
			File destinationFile = new File("D:\\RMITOOL_WD\\");
			File[] listFiles = sourceDir.listFiles();
			
			StringBuffer rbCmd = new StringBuffer();
			for(int a=0;a<listFiles.length;a++)
			{
				rbCmd = new StringBuffer();
				rbCmd.append("robocopy \""+sourceDir+"\" \""+destinationFile+"\" \""+listFiles[a].getName()+"\" ");
				Runtime r = Runtime.getRuntime();
				Process p = r.exec("robocopy \""+sourceDir+"\" \""+destinationFile+"\" \""+listFiles[a].getName()+"\" ");
				p.waitFor();
				p.destroy();
				rbCmd = null;
//				FileUtils.copyFileToDirectory(listFiles[a], destinationFile);
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
