package com.chops.chopsaccount;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;


/*  진척상황
*200209
 - 디비 생성했고, 어떻게 쓰는 지 테스트함.
 - select하고 insert테스트
 - 스크롤뷰에 뿌려줘야 하고, 리니어 클릭했을 때의 이벤트(테스트뷰에 아이디를 넣어줘. 그걸 select하면 되겠다)

 *200211
 - https://lovefields.github.io/android/2017/03/16/post27.html : 포커스에 대한 자세한 설명
   # 키보드 숨기기. 하지만 다른 곳을 클릭하면 클릭 소리가 나거나, scrollview 클릭하면 안먹히는 등의 문제가 있음.
 - http://www.masterqna.com/android/37356/edittext-%EC%88%98%ED%96%89%EC%8B%9C-%ED%82%A4%EB%B3%B4%EB%93%9C%EA%B0%80-%EB%9C%A8%EB%8A%94%EB%8D%B0-%ED%99%94%EB%A9%B4%EC%9D%84-%ED%84%B0%EC%B9%98-%ED%96%88%EC%9D%84-%EB%95%8C-%ED%82%A4%EB%B3%B4%EB%93%9C%EB%A5%BC-%EC%82%AC%EB%9D%BC%EC%A7%80%EA%B2%8C-%ED%95%98%EB%8A%94-%EB%B0%A9%EB%B2%95

 * 200213
 - 삭제 버튼 만들어야 하고, 수정 바인딩 해야함. 그리고 입력테스트도 진행해야 함.

 * 200214
 - 좋아. 포커스가 빠지긴 해. 하지만  scrollview에서는 포커스가 안빠져..
 - 달력 이상함.
*/
public class MainActivity extends AppCompatActivity
{
    //~부터 날짜
    private TextView mtvFDate;
    private DatePickerDialog.OnDateSetListener mdpFirstCallBack;
    //~까지 날짜
    private TextView mtvLDate;
    private DatePickerDialog.OnDateSetListener mdpLastCallBack;

    //신규or수정 TextView;
    private TextView mtvInput;
    //입력 seq
    private String mstrSeq;
    //입력날짜
    private EditText metDate;
    private DatePickerDialog.OnDateSetListener mdpInputDateCallBack;
    //은행
    private EditText metBank;
    //구분
    private EditText metGubun;
    //내용
    private EditText metContents;
    //수입rdo
    private RadioButton mrdoIncome;
    //지출rdo
    private RadioButton mrdoExpense;
    //금액
    private EditText metMoney;


    //DB관련
    private DatabaseHelper mdbHelper;
    private SQLiteDatabase mdb;

    View.OnClickListener linearClick;
    //et의 키보드를 내리자.
    View.OnFocusChangeListener mliKeyboardDown;

    //키보드 컨트롤
    private InputMethodManager mIMM;

    @Override
    //region #####안드로이드 연결#####
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //키보드 나올 때 ui 안가리게 하기
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        //노티 권한 관련.
        boolean isPermissionAllowed = isNotiPermissionAllowed();
        if(!isPermissionAllowed) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }

        mdbHelper = new DatabaseHelper(this);
        try{
            mdb = mdbHelper.getReadableDatabase();
        }catch (SQLException e){
            mdb = mdbHelper.getWritableDatabase();
        }

        this.mfnCallBackListener();
        this.mfnInitControl();

    }

    //콜백함수
    private void mfnCallBackListener()
    {

        mdpFirstCallBack = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mtvFDate.setText(mfnIntToStrDate(year)
                        + "/" + mfnIntToStrDate(month + 1)
                        + "/" + mfnIntToStrDate(dayOfMonth));
                if(!mfnCompareStrDate(mtvLDate,year,month + 1,dayOfMonth,true))
                    mtvLDate.setText(mtvFDate.getText().toString());
                mfnDBReadAccountList();
            }
        };

        mdpLastCallBack = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mtvLDate.setText(mfnIntToStrDate(year)
                        + "/" + mfnIntToStrDate(month + 1)
                        + "/" + mfnIntToStrDate(dayOfMonth));
                if(!mfnCompareStrDate(mtvFDate,year,month + 1,dayOfMonth,false))
                    mtvFDate.setText(mtvLDate.getText().toString());
                mfnDBReadAccountList();
            }
        };

        mdpInputDateCallBack = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                metDate.setText(mfnIntToStrDate(year)
                        + "/" + mfnIntToStrDate(month + 1)
                        + "/" + mfnIntToStrDate(dayOfMonth));
                mfnDBReadAccountList();
            }
        };

        //가계부 클릭 시, 이벤트를 자바에서 만들 때..
        linearClick = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mfnAccountListClick(v);
            }
        };

        //키보드 내리자.
        mIMM = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mliKeyboardDown = new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean bFocus) {
                View viCurrentView = getCurrentFocus();
                if(!bFocus && !(getCurrentFocus() instanceof EditText))
                {
                    mIMM.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                else
                {
                    mIMM.showSoftInput(view,0);
                }
            }
        };

    }



    //컨트롤 초기화
    private void mfnInitControl()
    {
        mtvFDate = (TextView)findViewById(R.id.fDate);
        mtvFDate.setText(mfnInitDateToToday());
        mtvLDate = (TextView)findViewById(R.id.lDate);
        mtvLDate.setText(mfnInitDateToToday());
        //Input 컨트롤
        mtvInput = (TextView)findViewById(R.id.tvInput);
        mstrSeq ="";
        metDate = (EditText)findViewById(R.id.etDate);
        metDate.setText(mfnInitDateToToday());
        metBank = (EditText)findViewById(R.id.etBank);
        metBank.setOnFocusChangeListener(mliKeyboardDown);
        metGubun = (EditText)findViewById(R.id.etGubun);
        metGubun.setOnFocusChangeListener(mliKeyboardDown);
        metContents = (EditText)findViewById(R.id.etContent);
        metContents.setOnFocusChangeListener(mliKeyboardDown);
        mrdoIncome = (RadioButton)findViewById(R.id.rdoIncome);
        mrdoIncome.setChecked(false);
        mrdoExpense = (RadioButton)findViewById(R.id.rdoExpense);
        mrdoExpense.setChecked(true);
        metMoney = (EditText)findViewById(R.id.etMoney);
        metMoney.setOnFocusChangeListener(mliKeyboardDown);

        mfnDBReadAccountList();
    }

    //노티권한주기
    private boolean isNotiPermissionAllowed()
    {
        Set<String> notiListenerSet = NotificationManagerCompat.getEnabledListenerPackages(this);
        String myPackageName = getPackageName();

        for(String packageName : notiListenerSet) {
            if(packageName == null) {
                continue;
            }
            if(packageName.equals(myPackageName)) {
                return true;
            }
        }
        return false;
    }

    //endregion 안드로이드연결_End

    //region #####내부함수#####
    //8자리의 날짜를 연도, 월, 일의 형태로 잘라 주는 것.
    private int mfnStrDateToInt(String p_strDate, String p_strYMD)
        //p_strDate : 8자리의 날짜
        //p_strYMD : Y일 경우 연도, M일 경우 월, D일 경우 일을 리턴
        //return : 형식에 맞게 변환된 날짜의 int형태를 반환한다.
    {
        //strYMD가 Y,M,D 중에 없는 경우
        if(!"YMD".contains(p_strYMD) || p_strYMD.length() != 1)
        {
            return 0;
        }
        //p_strDate는 8자리의 년월일으로 들어와야 함.
        else if(p_strDate.length() != 8 )
        {
            return 0;
        }
        try
        {
            if(p_strYMD.equals("Y"))
            {
                return Integer.parseInt(p_strDate.substring(0,4));
            }
            else if(p_strYMD.equals("M"))
            {
                return Integer.parseInt(p_strDate.substring(4,6));
            }
            else if(p_strYMD.equals("D"))
            {
                return Integer.parseInt(p_strDate.substring(6,8));
            }
            else
            {
                return 0;
            }
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    //Int를 String형식의 Date로 바꿔준다.
    private String mfnIntToStrDate(int p_iDate)
        //p_strYMD : Y일 경우 연도(xxxx), M일 경우 월(xx), D일 경우 일(xx)을 리턴
    {
        return String.format(Locale.getDefault(),"%02d", p_iDate);
    }

    //yyyyMMdd ->을 yyyy/MM/dd형식으로 바꿔줌.
    private String mfnMakeStrDate(String p_strDate)
    {
        //p_strDate : yyyyMMdd
        //return : yyyy/MM/dd
        return p_strDate.substring(0,4) + "/" + p_strDate.substring(4,6) + "/" + p_strDate.substring(6,8);
    }

    //두 8자리의 Date 스트링을 비교한다.
    private boolean mfnCompareStrDate(TextView p_tvDate, int p_iYear, int p_iMonth, int p_iDay, boolean p_isFirstBig)
        //p_tvDate : yyyy/MM/dd형태의 TextView
        //p_iYear,p_iMonth,p_iDay : 연도,월,일
        //p_isFirstBig : 앞에게 크다면 true
    {
        String strFirstDate = p_tvDate.getText().toString().replace("/","");
        String strSecondDate = mfnIntToStrDate(p_iYear) + mfnIntToStrDate(p_iMonth) + mfnIntToStrDate(p_iDay);

        if (strFirstDate.length() != 8 || strSecondDate.length() != 8)
            return false;

        try
        {
            if (Integer.parseInt(strFirstDate) > Integer.parseInt(strSecondDate))
                return p_isFirstBig;
            else if (Integer.parseInt(strFirstDate) < Integer.parseInt(strSecondDate))
                return !p_isFirstBig;
            else if (Integer.parseInt(strFirstDate) == Integer.parseInt(strSecondDate))
                return true;
            else
                return false;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    //오늘 날짜로 초기화한다.
    private String mfnInitDateToToday()
    {
        Date dtToday = Calendar.getInstance().getTime();
        SimpleDateFormat sdfYMD = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        return sdfYMD.format(dtToday);
    }

    //날짜를 하루 앞으로, 뒤로 옮긴다.
    private String mfnDayMove(String p_strDate, int p_iDay)
        //p_strDate : yyyy/MM/dd 형태의 strDate
        //p_iDay : 몇 일을 옮길지
    {
        SimpleDateFormat transFormat = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        try
        {
            cal.setTime(transFormat.parse(p_strDate));
        }
        catch (Exception e)
        {
            return "";
        }
        cal.add(Calendar.DATE, p_iDay);

        return transFormat.format(cal.getTime());
    }

    //동적으로 가계부 내역 만들기
    private void mfnDynamicContentsMaker(String p_Seq, String p_strDate, String p_strContents, String p_strMoney)
        //p_strDate : MMdd
        //p_strContents : 내용
        //p_strMoney : 수/지dynamic_contents
    {

        //날짜 TextView 생성
        TextView tvDay = new TextView(this);
        tvDay.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics()));
        tvDay.setHeight(ActionBar.LayoutParams.WRAP_CONTENT);
        tvDay.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),0,0,0);
        tvDay.setSingleLine(true);
        tvDay.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setText(p_strDate);
        tvDay.setTag("tvDate");

        //내용 TextView 생성
        TextView tvContents = new TextView(this);
        //tvContents.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 230, getResources().getDisplayMetrics()));
        //tvContents.setHeight(ActionBar.LayoutParams.WRAP_CONTENT);
        tvContents.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),0,0,0);
        tvContents.setSingleLine(true);
        tvContents.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tvContents.setGravity(Gravity.LEFT);
        tvContents.setText(p_strContents);
        tvContents.setTag("tvContents");
        tvContents.setLayoutParams(new TableLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, 1f));

        //수/지 TextView 생성
        TextView tvMoney = new TextView(this);
        tvMoney.setWidth((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics()));
        tvMoney.setHeight(ActionBar.LayoutParams.WRAP_CONTENT);
        tvMoney.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),0,(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics()),0);
        tvMoney.setSingleLine(true);
        tvMoney.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tvMoney.setGravity(Gravity.RIGHT);
        tvMoney.setText(p_strMoney);
        tvMoney.setTag("tvMoney");

        //TextView들을 담을 LinearLayout 생성
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        lp.bottomMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(lp);
        ll.setTag(p_Seq);
        ll.setOnClickListener(linearClick);


        ll.addView(tvDay);
        ll.addView(tvContents);
        ll.addView(tvMoney);

        //최종적으로 추가
        LinearLayout pll = findViewById(R.id.dynamic_contents);
        pll.addView(ll);
    }

    //string Money에 콤마를 찍자.
    private String mfnMoneyComma(String p_strMoney)
    {
        //p_strMoney : xxxxxxxx 라는 string 숫자
        //result : xx,xxx,xxx

        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(Integer.parseInt(p_strMoney));
    }


    //디비를 읽자 클릭이벤트
    private void mfnDBReadAccountList()
    {
        //뷰 초기화하고
        LinearLayout pll = findViewById(R.id.dynamic_contents);

        while (true)
        {
            if (pll.getChildCount() > 1)
            {
                pll.removeViews(1,1);
            }
            else
                break;
        }

        String str = "";
        str =  "SELECT * ";
        str += "FROM AccountList ";
        str += "WHERE Date BETWEEN '" + mtvFDate.getText().toString().replace("/","") + "'";
        str += "               AND '" + mtvLDate.getText().toString().replace("/","") + "'";
        str += "  AND Valid = 'Y'";
        Cursor cu = mdb.rawQuery(str, null);
        String strSeq = "";
        String strDate = "";
        String strContents = "";
        String strMoney = "";
        int iSum = 0;
        while (cu.moveToNext())
        {
            for(int i = 0; i < cu.getColumnCount(); i++)
            {
                if (cu.getColumnName(i).toLowerCase().equals("seq"))
                    strSeq = cu.getString(i);
                else if (cu.getColumnName(i).toLowerCase().equals("date"))
                    strDate = cu.getString(i).substring(4,8);
                else if (cu.getColumnName(i).toLowerCase().equals("contents"))
                    strContents = cu.getString(i);
                else if (cu.getColumnName(i).toLowerCase().equals("money")) {
                    strMoney = mfnMoneyComma(cu.getString(i));
                    iSum += Integer.parseInt(cu.getString(i));
                }
            }
            mfnDynamicContentsMaker(strSeq, strDate, strContents, strMoney);
        }
        TextView tv = (TextView)findViewById(R.id.tvSum);
        tv.setText(mfnMoneyComma(Integer.toString(iSum)));
    }

    //입력
    private void mfnInput(boolean p_isNew)
    {
        if (metDate.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(this,"날짜 입력 오류", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (metContents.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(this,"내용 입력 오류", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (metMoney.getText().toString().replace(" ","").equals(""))
        {
            Toast.makeText(this,"금액 입력 오류", Toast.LENGTH_SHORT).show();
            return;
        }
        else if ((mrdoExpense.isChecked() && mrdoIncome.isChecked()) || (!mrdoExpense.isChecked() && !mrdoIncome.isChecked()))
        {
            Toast.makeText(this,"수입,지출 입력 오류", Toast.LENGTH_SHORT).show();
            return;
        }


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //삽입
        String strSql = "";
        String strDate = metDate.getText().toString().replace("/","");
        String strBank = metBank.getText().toString();
        String strGubun =  metGubun.getText().toString();
        String strContents = metContents.getText().toString();
        String strMoney = (mrdoIncome.isChecked() ? "" : "-") + metMoney.getText().toString().replace("-","");

        if(p_isNew)
        {
            strSql += " INSERT INTO AccountList ('Date', 'Kinds', 'Classification', 'Contents', 'Money','Valid', 'Add_Date', 'Upd_Date')";
            strSql += " VALUES('" + strDate + "', '" + strBank + "', '" + strGubun + "', '" + strContents + "', " + strMoney + ", 'Y', '" + sdf.format(System.currentTimeMillis()) + "', '" + sdf.format(System.currentTimeMillis()) + "');";
        }
        else
        {
            strSql += " UPDATE AccountList";
            strSql += "    SET Date = '" + strDate + "'";
            strSql += "      , Kinds = '" + strBank + "'";
            strSql += "      , Classification = '" + strGubun + "'";
            strSql += "      , Contents = '" + strContents + "'";
            strSql += "      , Money = '" + strMoney + "'";
            strSql += "      , Upd_Date = '" + sdf.format(System.currentTimeMillis()) + "'";
            strSql += "  WHERE Seq = '" + mstrSeq +"'";
        }

        mdb.execSQL(strSql);

        Toast.makeText(this, "저장성공", Toast.LENGTH_SHORT).show();

        mfnDBReadAccountList();
        //입력이 끝나면 초기화.
        mfnResetControl();

    }

    //컨트롤 초기상태로 리셋
    private void mfnResetControl()
    {
        mstrSeq = "";
        metBank.setText("");
        metGubun.setText("");
        metContents.setText("");
        metMoney.setText("");
        mtvInput.setText("※ 신규");
    }

    private void mfnDel()
    {
        if(!mstrSeq.equals(""))
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String strSql = "";
            strSql += " UPDATE AccountList";
            strSql += "    SET Valid = 'N'";
            strSql += "      , Upd_Date = '" + sdf.format(System.currentTimeMillis()) + "'";
            strSql += "  WHERE Seq = '" + mstrSeq + "'";
            mdb.execSQL(strSql);
        }
        Toast.makeText(this, "삭제성공", Toast.LENGTH_SHORT).show();
        mfnResetControl();
        mfnDBReadAccountList();
    }

    //endregion 내부함수_End

    //region #####클릭이벤트#####
    //데이트피커 클릭
    public void mfnDatePickerClick(View view)
    {
        //DatePicker 객체선언
        DatePickerDialog dialog;
        //TextView의 날짜를 DatePick에 넣어주기 위한 변수
        String strDate ="";

        if(R.id.fDate == view.getId())
        {
            strDate = mtvFDate.getText().toString().replace("/","");
            dialog = new DatePickerDialog(this, mdpFirstCallBack
                , mfnStrDateToInt(strDate,"Y")
                , mfnStrDateToInt(strDate,"M") - 1
                , mfnStrDateToInt(strDate,"D"));
            dialog.show();
            mfnDBReadAccountList();
        }
        else if(R.id.lDate == view.getId())
        {
            strDate = mtvLDate.getText().toString().replace("/","");
            dialog = new DatePickerDialog(this, mdpLastCallBack
                    , mfnStrDateToInt(strDate,"Y")
                    , mfnStrDateToInt(strDate,"M") - 1
                    , mfnStrDateToInt(strDate,"D"));
            dialog.show();
            mfnDBReadAccountList();
        }
        else if (R.id.etDate == view.getId())
        {
            strDate = mtvLDate.getText().toString().replace("/","");
            dialog = new DatePickerDialog(this, mdpInputDateCallBack
                    , mfnStrDateToInt(strDate,"Y")
                    , mfnStrDateToInt(strDate,"M") - 1
                    , mfnStrDateToInt(strDate,"D"));
            dialog.show();
        }

    }

    //Today 클릭
    public void mfnTodayClick(View view)
    {
        mtvFDate.setText(mfnInitDateToToday());
        mtvLDate.setText(mfnInitDateToToday());
        mfnDBReadAccountList();
    }

    //전날 클릭
    public void mfnPreviousDayClick(View view)
    {
        mtvFDate.setText(mfnDayMove(mtvFDate.getText().toString(),-1));
        mtvLDate.setText(mfnDayMove(mtvLDate.getText().toString(),-1));
        mfnDBReadAccountList();
    }

    //다음날 클릭
    public void mfnNextDayClick(View view)
    {
        mtvFDate.setText(mfnDayMove(mtvFDate.getText().toString(),1));
        mtvLDate.setText(mfnDayMove(mtvLDate.getText().toString(),1));
        mfnDBReadAccountList();
    }


    //가계부 리스트 클릭
    public void mfnAccountListClick(View view)
    {
        String strSeq = (String)view.getTag();
        String strSql = "";
        strSql += " SELECT Seq, Date, Kinds, Classification, Contents, Money";
        strSql += "   FROM AccountList";
        strSql += "  WHERE Seq = '" + strSeq + "'";

        Cursor cu = mdb.rawQuery(strSql,null);

        while (cu.moveToNext())
        {
            for(int i = 0; i < cu.getColumnCount(); i++)
            {
                if(cu.getColumnName(i).equals("Seq"))
                    mstrSeq = cu.getString(i);
                else if (cu.getColumnName(i).equals("Date"))
                    metDate.setText(mfnMakeStrDate(cu.getString(i)));
                else if (cu.getColumnName(i).equals("Kinds"))
                    metBank.setText(cu.getString(i));
                else if (cu.getColumnName(i).equals("Classification"))
                    metGubun.setText(cu.getString(i));
                else if (cu.getColumnName(i).equals("Contents"))
                    metContents.setText(cu.getString(i));
                else if (cu.getColumnName(i).equals("Money")) {
                    metMoney.setText(cu.getString(i).replace("-",""));
                    if(cu.getString(i).contains("-"))
                    {
                        mrdoExpense.setChecked(true);
                        mrdoIncome.setChecked(false);
                    }
                    else
                    {
                        mrdoExpense.setChecked(false);
                        mrdoIncome.setChecked(true);
                    }

                }
            }
            mtvInput.setText("※ 수정");
        }
    }

    //저장버튼
    public void mfnInputClick(View view)
    {
        mfnInput(mtvInput.getText().toString().equals("※ 신규"));

    }
    //신규버튼
    public void mfnNewClick(View view)
    {
        mfnResetControl();
    }

    //삭제버튼
    public void mfnDelClick(View view)
    {
        mfnDel();
    }

    //endregion 클릭이벤트_End
//테스트용 클릭이벤트
    public void TEST1(View view)
    {
        //테이블 지우는 거
        //mdbHelper.onUpgrade(mdb,1,1);
        Cursor cu = mdb.rawQuery("SELECT * FROM AccountList",null);
        String str = "";
        while (cu.moveToNext())
        {
            for(int i = 0; i < cu.getColumnCount(); i++)
            {
                str += cu.getString(i) + " ";
            }
        }
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();

    }
/*
    //테스트용 클릭이벤트
    public void TEST2(View view)
    {
        //테이블 지우는 거
        mdbHelper.onUpgrade(mdb,1,1);
    }
*/

}
