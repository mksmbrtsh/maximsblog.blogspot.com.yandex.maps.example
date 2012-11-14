package com.example.maximsblog.blogspot.com.yandex.maps.example;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
/*
 * класс для работы с координатой
 * в ° градусах в виде десятичной дроби (современный вариант)
 */
public class Coordinate implements Parcelable {

	// неправильное значение координаты
	private static final double INVALID_COORDINATE = -1000.0;
	// текущее значение координаты
	private double mValue;
	
	// конструкторы
	public Coordinate(){
		mValue = INVALID_COORDINATE;
	}
	
	public Coordinate(double c){
		mValue = c;
	}
	
	public Coordinate(String s){
		try {
			// жестко задан разделитель целой и дробной части
			String decimalSeparator = ".";
			mValue = Double.parseDouble(s.replace(".", decimalSeparator));
			if(isValid()){
				
			} else
				mValue = INVALID_COORDINATE;
		} catch (NumberFormatException e) {
			mValue = INVALID_COORDINATE;
		}
	}
	
	public Coordinate(Parcel in) {
		mValue = in.readDouble();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	/*
	 * проверка на правильность значения координаты
	 */
	public boolean isValid(){
		return (mValue > INVALID_COORDINATE);
	}
	@Override
	public String toString() {
		// разделитель в координатах - точка
		// см. http://ru.wikipedia.org/wiki/%D0%93%D0%B5%D0%BE%D0%B3%D1%80%D0%B0%D1%84%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B8%D0%B5_%D0%BA%D0%BE%D0%BE%D1%80%D0%B4%D0%B8%D0%BD%D0%B0%D1%82%D1%8B
		DecimalFormat nf = new DecimalFormat();
		nf.applyPattern("###.##########");
		nf.setMinimumFractionDigits(4);
		nf.setMaximumFractionDigits(10);
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		String decimalSeparator = String.valueOf(dfs.getDecimalSeparator());
		return nf.format(mValue).replace(decimalSeparator, ".");
	};
	
	public double GetValue(){
		return mValue;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDouble(mValue);
	}
	
	public static final Creator<Coordinate> CREATOR = new Creator<Coordinate>() {
	    public Coordinate createFromParcel(Parcel in) {
	        return new Coordinate(in);
	    }

	    public Coordinate[] newArray(int size) {
	        return new Coordinate[size];
	    }
	};

}
