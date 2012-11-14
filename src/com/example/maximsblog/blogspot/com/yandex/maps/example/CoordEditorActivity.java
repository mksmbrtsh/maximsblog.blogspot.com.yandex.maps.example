package com.example.maximsblog.blogspot.com.yandex.maps.example;

import java.util.regex.Pattern;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import ru.yandex.yandexmapkit.MapController;
import ru.yandex.yandexmapkit.MapView;
import ru.yandex.yandexmapkit.OverlayManager;
import ru.yandex.yandexmapkit.overlay.OverlayItem;
import ru.yandex.yandexmapkit.overlay.drag.DragAndDropItem;
import ru.yandex.yandexmapkit.overlay.drag.DragAndDropOverlay;
import ru.yandex.yandexmapkit.utils.GeoPoint;
import ru.yandex.yandexmapkit.utils.ScreenPoint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

public class CoordEditorActivity extends Activity {

	private EditText mEditX;
	private EditText mEditY;
	private MapView mMapView;

	private OverlayItem mTapItem;
	private customOverlay mOverlay;
	private MapController mMapController;
	private OverlayManager mOverlayManager;

	private Coordinate mX;
	private Coordinate mY;
	private String mDecimalSeparator;
	private Bitmap mTapItemBitmap;

	private boolean mFlgBlockedTextChange = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.coordeditor_activity);
		mEditX = (EditText) findViewById(R.id.editX);
		mEditY = (EditText) findViewById(R.id.editY);
		mMapView = (MapView) findViewById(R.id.map);
		// установка цифровой клавиатуры для полей ввода
		mEditX.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
		mEditX.setRawInputType(Configuration.KEYBOARD_12KEY);
		mEditY.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
		mEditY.setRawInputType(Configuration.KEYBOARD_12KEY);
		// точка, как разделитель целой и дробной части градусов
		// и для строкового представления double
		// проблема на телефонах самсунга, там цифровая клавиатура не реализует
		// правильное поведение разделителя дробной части DecimalSeparator
		// в зависимости от текущей локали
		// http://developer.samsung.com/forum/board/thread/view.do?boardName=GeneralB&messageId=164339&startPage=14&curPage=16
		mDecimalSeparator = ".";
		// установка фильтра для полей ввода, вводить можно только координаты
		mEditX.setFilters(new InputFilter[] { new CoordsInputFilter() });
		mEditY.setFilters(new InputFilter[] { new CoordsInputFilter() });
		if (savedInstanceState == null) {
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			// по умолчанию ставятся координаты пригорода Парижа
			mX = new Coordinate(pref.getString("x", "2.24"));
			mY = new Coordinate(pref.getString("y", "48.44"));
		} else {
			// восстановление координат
			mX = savedInstanceState.getParcelable("geox");
			mY = savedInstanceState.getParcelable("geoy");
		}
		// установка в поля ввода текущих координат
		if (mX.isValid())
			mEditX.setText(mX.toString().replace(".", mDecimalSeparator));
		if (mY.isValid())
			mEditY.setText(mY.toString().replace(".", mDecimalSeparator));
		// текст меняется - меняется и место на карте
		mEditX.addTextChangedListener(mXWatcher);
		mEditY.addTextChangedListener(mYWatcher);
		// Получаем MapController, настраиваем
		mMapController = mMapView.getMapController();
		mOverlayManager = mMapController.getOverlayManager();
		mMapController.setZoomCurrent(10);
		// создание своего слоя
		mOverlay = new customOverlay(mMapController);
		mOverlayManager.addOverlay(mOverlay);
		// установка тайла(метки)
		setManualItem();
	}

	/*
	 * Самодельный слой, наследуется от слоя по которому можно тыкать и тащить
	 */
	private class customOverlay extends DragAndDropOverlay {

		public customOverlay(MapController arg0) {
			super(arg0);
		}

		@Override
		public boolean onSingleTapUp(float arg0, float arg1) {
			if (mTapItem != null) {
				// удаление старой метки(тайла)
				mOverlay.removeOverlayItem(mTapItem);
			}
			// получение координат экрана в рамках вьювера карты
			ScreenPoint s = new ScreenPoint(arg0, arg1);
			// преобразование экранных координат в географические
			GeoPoint g = mMapController.getGeoPoint(s);
			// создание и добавление новой метки(тайла)
			mTapItem = new DragAndDropItem(g, mTapItemBitmap);
			mOverlay.addOverlayItem(mTapItem);
			mMapController.setPositionNoAnimationTo(g);
			// переозначивание координат
			mX = new Coordinate(mTapItem.getGeoPoint().getLon());
			mY = new Coordinate(mTapItem.getGeoPoint().getLat());
			// взведение флага блокировки для предотвращения
			// рекурсивного вызова ручного обновления координат
			mFlgBlockedTextChange = true;
			if (mX.isValid())
				mEditX.setText(mX.toString().replace(".", mDecimalSeparator));
			if (mY.isValid())
				mEditY.setText(mY.toString().replace(".", mDecimalSeparator));
			mFlgBlockedTextChange = false;
			return super.onSingleTapUp(arg0, arg1);
		};

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 * сохраниение координат при изменении конфигурации
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable("geox", new Coordinate(mEditX.getText()
				.toString().replace(mDecimalSeparator, ".")));
		outState.putParcelable("geoy", new Coordinate(mEditY.getText()
				.toString().replace(mDecimalSeparator, ".")));
		super.onSaveInstanceState(outState);
	};

	/*
	 * вспомогательный класс для фильстрации вводимых чисел по маске координат
	 */
	private class CoordsInputFilter implements InputFilter {

		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {

			String checkedText = dest.subSequence(0, dstart).toString()
					+ source.subSequence(start, end)
					+ dest.subSequence(dend, dest.length()).toString();
			String pattern = getPattern();
			if (!Pattern.matches(pattern, checkedText)) {
				return "";
			}

			return null;
		}

		// маска для ввода координат от -999.9999999999 до 999.9999999999
		private String getPattern() {
			String pattern = "\\+?\\-?[0-9]{0,3}+([" + mDecimalSeparator
					+ "]{1}||[" + mDecimalSeparator + "]{1}[0-9]{0,10})?";
			return pattern;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy() при уничтожении активити сохранение
	 * координат в преференс приложения
	 */
	@Override
	protected void onDestroy() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = pref.edit();
		editor.putString("x", mX.toString());
		editor.putString("y", mY.toString());
		editor.commit();
		super.onDestroy();
	}

	/*
	 * метод ручной установки метки(тайла) на карте
	 */
	private void setManualItem() {
		// установка координат в ручную
		if (mX.isValid() && mY.isValid()) {
			if (mTapItem != null) {
				// удаление старого тайла(метки), если она есть
				mOverlay.removeOverlayItem(mTapItem);
			}
			GeoPoint g = new GeoPoint(mY.GetValue(), mX.GetValue());
			// перемещаем карту на заданные координаты
			mMapController.setPositionNoAnimationTo(g);
			mMapController.setZoomCurrent(10);
			Resources res = getResources();
			mTapItemBitmap = BitmapFactory
					.decodeResource(res, R.drawable.drag1);
			// создание нового тайла(метки) на карте
			mTapItem = new DragAndDropItem(g, mTapItemBitmap);
			mOverlay.addOverlayItem(mTapItem);
			mOverlayManager.getMyLocation().setEnabled(false);
		}
	}

	private TextWatcher mXWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

			if (!mFlgBlockedTextChange) {
				mX = new Coordinate(s.toString());
				setManualItem();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {

		}
	};
	private TextWatcher mYWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			if (!mFlgBlockedTextChange) {
				mY = new Coordinate(s.toString());
				setManualItem();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case (R.id.main_menu): 
				Builder b = new Builder(this);
			b.setMessage(getString(R.string.ya));
			b.create().show();
			break;
		}
		return true;
	}

}
