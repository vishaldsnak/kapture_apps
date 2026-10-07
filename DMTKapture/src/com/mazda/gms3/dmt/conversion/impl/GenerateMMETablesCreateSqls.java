package com.mazda.gms3.dmt.conversion.impl;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;

import com.mazda.gms3.dmt.utils.FileReadWriteUtil;

public class GenerateMMETablesCreateSqls {


	/**
	 * Function to generate DMT Tables Sqls
	 * @param args
	 */
	public static void main_1(String[] args) {
		try
		{
			File sqlFolders = PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\Phase 1.3\\DMT_DDLs\\EN_UK");
			File[] listFiles = sqlFolders.listFiles();
			String locales="cs_CZ,pl_PL,sv_SE,de_DE,fi_FI,pt_PT,tr_TR,fr_FR,el_GR,ru_RU,nl_NL,it_IT,es_ES";
			String[] tokens=  locales.split(",");
			
			for(int a=0;a<tokens.length;a++)
			{
				String loc = tokens[a].toUpperCase();
				for(int b=0;b<listFiles.length;b++)
				{
					File sqlFile = listFiles[b];
					if(sqlFile.isFile())
					{
						/*
						 * read Data of the Sql File
						 * and replace all EN_UK BY 
						 */
						byte[] data = FileReadWriteUtil.readFile(PathUtil.winPath(sqlFile));
						if(null!=data)
						{
							String content = new String(data,"UTF-8");
							content = content.replace("EN_UK", loc);
							data = content.getBytes("UTF-8");
							String newPath = PathUtil.winPath(sqlFile.getParentFile());
							newPath = newPath.replace("EN_UK", loc);
							File newFile = PathUtil.file(newPath);
							if(newFile.isDirectory()==false && newFile.exists()==false)
							{
								newFile.mkdir();
							}
							newFile = null;
							// append File Name
							newPath = newPath+"\\"+sqlFile.getName();
							newPath = newPath.replace("EN_UK", loc);
							FileReadWriteUtil.writeFile(data, newPath);
							newPath = null;
							content = null;
						}
						data = null;
					}
					sqlFile= null;
				}
				loc = null;
			}
			listFiles=null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Function to generate VC Tables Sqls
	 */
	public static void main(String[] args) {
		try
		{
			File sqlFolders = PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\Phase 1.3\\DMT_DDLs\\VC");
			File[] listFiles = sqlFolders.listFiles();
			String locales="cs_CZ,pl_PL,sv_SE,de_DE,fi_FI,pt_PT,tr_TR,fr_FR,el_GR,ru_RU,nl_NL,it_IT,es_ES";
			String[] tokens=  locales.split(",");
			for(int b=0;b<tokens.length;b++)
			{
				String loc = tokens[b].toUpperCase();
				for(int a=0;a<listFiles.length;a++)
				{
					File sqlFile = listFiles[a];
					if(sqlFile.isFile() && sqlFile.getName().contains("13 - gms3_vc_mme_vin_dtl_en_uk"))
					{
						byte[] data = FileReadWriteUtil.readFile(PathUtil.winPath(sqlFile));
						if(null!=data)
						{
							String content = new String(data,"UTF-8");
							content = content.replace("EN_UK", loc);
							data = content.getBytes("UTF-8");
							String newPath = PathUtil.winPath(sqlFile);
							newPath = newPath.replace("EN_UK", loc);
							FileReadWriteUtil.writeFile(data, newPath);
							newPath = null;
							content = null;
						}
						data = null;
					}
					sqlFile = null;
				}
				loc=  null;
			}
			listFiles=null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	

}
