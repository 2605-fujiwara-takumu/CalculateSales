package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		final String fileNameRegex = "^[0-9]{8}\\.rcd$"; //メソッドの中では変数は小文字のほうがいい
		final String salesAmountRegex = "^[0-9]+$";
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		// 条件（数字8桁.rcd）に一致するファイル情報をリストに追加
		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() && files[i].getName().matches(fileNameRegex)) {
				rcdFiles.add(files[i]);
			}
		}

		//売上ファイルが連番か確認
		Collections.sort(rcdFiles);

		for(int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println("売上ファイル名が連番になっていません。");
				return;
			}
		}

		//売上ファイルの情報を支店ごとに格納
		//同じ支店の売上ファイルが複数ある場合は、金額を加算してから格納
		BufferedReader br = null;

		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line; //変数名:lineに変更
				List<String> fileContents = new ArrayList<>();

				 //１ファイルずつ読んでListに格納する
				while((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				//売上ファイルの行数が2行か確認
				if(fileContents.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + "のフォーマットが不正です。");
					return;
				}

				//売上ファイルの支店コード確認
				if (!branchNames.containsKey(fileContents.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + "の支店コードが不正です。");
					return;
				}

				//売上金額が数字か確認
				if(!fileContents.get(1).matches(salesAmountRegex)) {
					System.out.println(UNKNOWN_ERROR);
				    return;
				}

				//Listを使って計算する
		        long fileSale = Long.parseLong(fileContents.get(1));
		        Long saleAmount = branchSales.get(fileContents.get(0)) + fileSale;

		      //売上金額が10桁以下か確認
		        if(saleAmount >= 10000000000L){
		        	System.out.println("合計⾦額が10桁を超えました。");
					return;
		        }

		        branchSales.put(fileContents.get(0), saleAmount);

			}catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} catch (Exception e) {
			    System.out.println(UNKNOWN_ERROR + e);
			    return;
			}finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					} catch (Exception e) {
					    System.out.println(UNKNOWN_ERROR + e);
					    return;
					}
				}
			}
		}
		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;
		String regex = "^[0-9]{3}$";
		final int expectedColumns = 2;

		try {
			File file = new File(path, fileName);

			//支店定義ファイルの存在確認
			if(!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			while((line = br.readLine()) != null) {

				//1行に支店コードと支店名が「,」(カンマ)で区切られているか確認
				if(!line.contains(",") ) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//処理内容1-2
				String[] items = line.split(",");

				//配列の要素数(2)、支店コードは数字3桁であることを確認
				if((items.length != expectedColumns) || (!items[0].matches(regex))){
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				//処理内容1-2
				branchNames.put(items[0], items[1]);
				branchSales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		}  catch (Exception e) {
		    System.out.println(UNKNOWN_ERROR + e);
		    return false;
		}finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				} catch (Exception e) {
				    System.out.println(UNKNOWN_ERROR + e);
				    return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path,fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for(String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		}  catch (Exception e) {
		    System.out.println(UNKNOWN_ERROR + e);
		    return false;
		}finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				} catch (Exception e) {
				    System.out.println(UNKNOWN_ERROR + e);
				    return false;
				}
			}
		}
		return true;
	}
}
