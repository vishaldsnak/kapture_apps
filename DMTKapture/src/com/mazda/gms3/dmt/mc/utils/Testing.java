package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;

import org.apache.log4j.Logger;

import com.mazda.gms3.dmt.logging.LogManager;

public class Testing {

	static Logger logger = LogManager.getLogger(ConversionUtils.class);
	
	
	public static void main(String[] args) {
		try
		{
			File foldersList = PathUtil.file("\\\\m2okfs10\\sourcecontentqa\\2byte_filename_sample\\original\\GMS3_Content\\MC\\ja-JP\\Suzuki_azwagon_mj\\WM\\FL0004\\E-W1J-4TE1_DJVU\\djvu");
			File[] istFile = foldersList.listFiles();
			String fileName="";
			for(int a=0;a<istFile.length;a++)
			{
				fileName = istFile[a].getName();
				System.out.println("fileName :: > "+ istFile[a].getName());
				logger.info("fileName :: > "+ istFile[a].getName());
				
				if(a==2)
				{
					break;
				}
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
