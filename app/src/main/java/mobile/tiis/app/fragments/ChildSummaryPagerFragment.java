package mobile.tiis.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.ganfra.materialspinner.MaterialSpinner;
import mobile.tiis.app.ChildDetailsActivity;
import mobile.tiis.app.R;
import mobile.tiis.app.adapters.PlacesOfBirthAdapter;
import mobile.tiis.app.adapters.SingleTextViewAdapter;
import mobile.tiis.app.adapters.VaccinationHistoryListAdapter;
import mobile.tiis.app.base.BackboneActivity;
import mobile.tiis.app.base.BackboneApplication;
import mobile.tiis.app.database.DatabaseHandler;
import mobile.tiis.app.database.SQLHandler;
import mobile.tiis.app.entity.Birthplace;
import mobile.tiis.app.entity.Child;
import mobile.tiis.app.entity.HealthFacility;
import mobile.tiis.app.entity.ImmunizationCardItem;
import mobile.tiis.app.entity.Place;
import mobile.tiis.app.entity.Status;
import mobile.tiis.app.util.ViewAppointmentRow;

/**
 *  Created by issymac on 25/01/16.
 */

public class ChildSummaryPagerFragment extends Fragment {

    private static final String ARG_POSITION = "position";

    private static final String VALUE = "value";

    private int position;

    private Date bdate;

    private long birthDatesDiff = 0;

    private Child currentChild;

    private String hf_id, child_id, birthplacestr, villagestr, hfstr, statusstr, gender_val, birthdate_val;

    private ArrayList<String> gender;

    private String localBarcode = "";

    private String tempIdOrig, firstnameOrig, lastnameOrig, birthdateOrig, motherFirOrig, motherLastOrig, phoneOrig, notesOrig, barcodeOrig,firstname2Orig;

    private int birthplaceOrig, villageOrig, healthFacOrig, statusOrig, genderOrig;

    private int notApplicablePos = -1;

    private List<Place> placeList;

    private List<Birthplace> birthplaceList;

    private List<HealthFacility> healthFacilityList;

    private List<Status> statusList;

    private String childId;

    private ArrayList<ViewAppointmentRow> var;

    private Thread thread;

    public String value;
    private boolean editable = false;

    public MaterialEditText metBarcodeValue, metSystemID, metFirstName, metNotesValue,metMiddleName, metLastName, metMothersFirstName, metMothersSurname, metPhoneNumber, metDOB;

    VaccinationHistoryListAdapter adapter;

    PlacesOfBirthAdapter spinnerAdapter;

    ListView lvImmunizationHistory;
    private Cursor mCursor;
    Button editButton, saveButton;

    MaterialSpinner ms, pobSpinner, villageSpinner, healthFacilitySpinner, statusSpinner;

    DatabaseHandler mydb;

    BackboneApplication app;

    List<String> place_names;

    final DatePickerDialog doBDatePicker = new DatePickerDialog();

    public static final long getDaysDifference(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        long difference = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return difference;
    }

    public static ChildSummaryPagerFragment newInstance(int position, String value) {
        ChildSummaryPagerFragment f = new ChildSummaryPagerFragment();
        Bundle b                    = new Bundle();
        b                           .putInt(ARG_POSITION, position);
        b                           .putString(VALUE, value);
        f                           .setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position    = getArguments().getInt(ARG_POSITION);
        value     = getArguments().getString(VALUE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup v;
        v = (ViewGroup) inflater.inflate(R.layout.fragment_child_summary, null);

        setUpView(v);

        gender = new ArrayList<>();
        gender.add("Male");
        gender.add("Female");

        spinnerAdapter = new PlacesOfBirthAdapter(ChildSummaryPagerFragment.this.getActivity(), R.layout.single_text_spinner_item_drop_down, gender);

        View header = (View) inflater.inflate(R.layout.childinfo_summary_header, null);

        metBarcodeValue     = (MaterialEditText) header.findViewById(R.id.met_barcode_value);
        metSystemID         = (MaterialEditText) header.findViewById(R.id.met_sysId_value);
        metFirstName        = (MaterialEditText) header.findViewById(R.id.met_fname_value);
        metMiddleName       = (MaterialEditText) header.findViewById(R.id.met_mname_value);
        metLastName         = (MaterialEditText) header.findViewById(R.id.met_surname_value);
        metMothersFirstName = (MaterialEditText) header.findViewById(R.id.met_mother_fname_value);
        metMothersSurname   = (MaterialEditText) header.findViewById(R.id.met_mother_surname_value);
        metPhoneNumber      = (MaterialEditText) header.findViewById(R.id.met_phone_value);
        metDOB              = (MaterialEditText) header.findViewById(R.id.met_dob_value);
        metDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editable) {
                    doBDatePicker.show(((Activity) getActivity()).getFragmentManager(), "DatePickerDialogue");
                    doBDatePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

                            Cursor vacinationCursor = mydb.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + SQLHandler.Tables.VACCINATION_EVENT +
                                            " where " + SQLHandler.VaccinationEventColumns.CHILD_ID + "=? and " +
                                            SQLHandler.VaccinationEventColumns.VACCINATION_STATUS + "= 'true'",
                                    new String[]{currentChild.getId()});
                            vacinationCursor.moveToFirst();
                            if (vacinationCursor.getInt(0) > 0) {
                                //TODO : Something has to be done here
                                final AlertDialog ad2 = new AlertDialog.Builder((Activity)getActivity()).create();
                                ad2.setTitle(getResources().getString(R.string.error_editing_dob));
                                ad2.setMessage(getResources().getString(R.string.error_message_editing_dob));
                                ad2.setButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ad2.dismiss();
                                    }
                                });
                                ad2.show();

                                return;
                            }


                            metDOB.setText((dayOfMonth < 10 ? "0" + dayOfMonth : dayOfMonth) + "-" + ((monthOfYear + 1) < 10 ? "0" + (monthOfYear + 1) : monthOfYear + 1) + "-"
                                    + year);
                            Calendar toCalendar = Calendar.getInstance();
                            toCalendar.set(year, monthOfYear, dayOfMonth);
                            bdate = toCalendar.getTime();
                        }

                    });
                }
            }
        });

        metNotesValue       = (MaterialEditText) header.findViewById(R.id.met_notes_value);

        ms                  = (MaterialSpinner) header.findViewById(R.id.spin_gender);
        pobSpinner          = (MaterialSpinner) header.findViewById(R.id.spin_pob);
        villageSpinner      = (MaterialSpinner) header.findViewById(R.id.spin_village);
        healthFacilitySpinner=(MaterialSpinner) header.findViewById(R.id.spin_health_facility);
        statusSpinner       = (MaterialSpinner) header.findViewById(R.id.spin_status);

        editButton          = (Button) header.findViewById(R.id.edit_button);
        saveButton          = (Button) header.findViewById(R.id.save_button);

        lvImmunizationHistory.addHeaderView(header);
        View appointmentTableHeader = inflater.inflate(R.layout.appointment_table_header, null);
        View appointmentTableFooter = inflater.inflate(R.layout.appointment_table_footer, null);
        lvImmunizationHistory.addHeaderView(appointmentTableHeader);
        lvImmunizationHistory.addFooterView(appointmentTableFooter);

        lvImmunizationHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });

        app = (BackboneApplication) ChildSummaryPagerFragment.this.getActivity().getApplication();
        mydb = app.getDatabaseInstance();

        mCursor = null;
        mCursor = mydb.getReadableDatabase().rawQuery("SELECT * FROM child WHERE " + SQLHandler.ChildColumns.ID + "=?",
                new String[]{String.valueOf(value)});

        if (mCursor.getCount() > 0) {
            mCursor.moveToFirst();
            currentChild = getChildFromCursror(mCursor);
            Log.d("issy", "gotten a child");
        }else{
            Log.d("issy", "cursor empty");
        }

        enableUserInputs(false);
        fillUIElements();

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableUserInputs(true);
                app.saveNeeded = true;
                editButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.GONE);
                if (checkDataIntegrityBeforeSave()) {
                    if (!localBarcode.equals(metBarcodeValue.getText().toString()) && !localBarcode.equals("")) {
                        showAlertThatChildHadABarcode();
                    } else {
                        saveChangedData();
                        enableUserInputs(false);
                        ChildDetailsActivity.changeTitle(metFirstName.getText().toString()+" "+metMiddleName.getText().toString()+" "+metLastName.getText().toString());
                    }
                }
            }
        });

        loadViewAppointementsTable(false);

        return v;
    }

    public void enableUserInputs(boolean fieldStatus){
        editable = fieldStatus;
        metBarcodeValue.setFocusableInTouchMode(fieldStatus);
        metSystemID     .setFocusableInTouchMode(fieldStatus);
        metFirstName    .setFocusableInTouchMode(fieldStatus);
        metMiddleName   .setFocusableInTouchMode(fieldStatus);
        metLastName     .setFocusableInTouchMode(fieldStatus);
        metNotesValue     .setFocusableInTouchMode(fieldStatus);

        metMothersFirstName     .setFocusableInTouchMode(fieldStatus);
        metMothersSurname       .setFocusableInTouchMode(fieldStatus);
        metPhoneNumber          .setFocusableInTouchMode(fieldStatus);
        metDOB                  .setFocusableInTouchMode(false);

        ms                      .setEnabled(fieldStatus);
        pobSpinner              .setEnabled(fieldStatus);
        villageSpinner          .setEnabled(fieldStatus);
        healthFacilitySpinner   .setEnabled(fieldStatus);
        statusSpinner           .setEnabled(fieldStatus);

        if(!fieldStatus){
            ms.setBaseColor(R.color.card_light_text);
            pobSpinner.setBaseColor(R.color.card_light_text);
            villageSpinner.setBaseColor(R.color.card_light_text);
            healthFacilitySpinner.setBaseColor(R.color.card_light_text);
            statusSpinner.setBaseColor(R.color.card_light_text);
        }else{
            ms.setBaseColor(R.color.black);
            pobSpinner.setBaseColor(R.color.black);
            villageSpinner.setBaseColor(R.color.black);
            healthFacilitySpinner.setBaseColor(R.color.black);
            statusSpinner.setBaseColor(R.color.black);
        }

    }

    private void loadVaccinationHistory(){
         ArrayList<ImmunizationCardItem> immunizationCardList;
         immunizationCardList = mydb.getImmunizationCard(childId);
    }

    private void loadViewAppointementsTable(Boolean b){
        DatabaseHandler this_database = app.getDatabaseInstance();
        SQLHandler handler = new SQLHandler();

        var = new ArrayList<ViewAppointmentRow>();
        String result = "";


        if (currentChild.getId() != null && !currentChild.getId().isEmpty()) {
            child_id = currentChild.getId();
            Log.d("ViewAppointment:", "Child_Id: " + child_id);

            Cursor cursor = null;
            cursor = this_database.getReadableDatabase().rawQuery(handler.SQLVaccinations, new String[]{child_id, child_id});

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        ViewAppointmentRow row = new ViewAppointmentRow();
                        row.setAppointment_id(cursor.getString(cursor.getColumnIndex("APPOINTMENT_ID")));
                        row.setVaccine_dose(cursor.getString(cursor.getColumnIndex("VACCINES")));
                        row.setSchedule(cursor.getString(cursor.getColumnIndex("SCHEDULE")));
                        row.setScheduled_date(cursor.getString(cursor.getColumnIndex("SCHEDULED_DATE")));
                        var.add(row);
                    } while (cursor.moveToNext());
                }
            }

        }

        adapter = new VaccinationHistoryListAdapter(ChildSummaryPagerFragment.this.getActivity(), var, app);
        lvImmunizationHistory.setAdapter(adapter);

    }

    public void setUpView(View v){
        lvImmunizationHistory = (ListView) v.findViewById(R.id.vaccination_history_list);
    }

    private void fillUIElements(){

        if (currentChild!=null){

            Log.d("issy", "child gotten "+currentChild.getFirstname1());
            if (currentChild.getBarcodeID() == null || currentChild.getBarcodeID().isEmpty()) {
                Toast.makeText(ChildSummaryPagerFragment.this.getActivity(), getString(R.string.empty_barcode), Toast.LENGTH_SHORT).show();
            }

            // TODO : Commented because in previous version they used two different activity for viewing child details
            // TODO: 29/02/16
//            app.setAdministerVaccineHidden(false);

            localBarcode = currentChild.getBarcodeID();

            metBarcodeValue     .setText(currentChild.getBarcodeID());
            barcodeOrig         = currentChild.getBarcodeID();
            tempIdOrig          = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.TEMP_ID));

            childId = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.ID));
            metSystemID.setText(childId);

            metFirstName.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME1)));
            metNotesValue.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.NOTES)));
            metMiddleName.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME2)));
            metLastName.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.LASTNAME1)));

            firstnameOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME1));
            firstname2Orig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME2));
            lastnameOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.LASTNAME1));

            bdate = BackboneActivity.dateParser(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.BIRTHDATE)));
            SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy");
            metDOB.setText(ft.format(bdate));
            birthdateOrig = ft.format(bdate);
            birthdate_val = ft.format(bdate);

            metMothersFirstName.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_FIRSTNAME)));
            metMothersSurname.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_LASTNAME)));

            motherFirOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_FIRSTNAME));
            motherLastOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_LASTNAME));

            phoneOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.PHONE));
            metPhoneNumber.setText(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.PHONE)));

            notesOrig = mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.NOTES));


            if (Boolean.parseBoolean(mCursor.getString(mCursor.getColumnIndex(SQLHandler.ChildColumns.GENDER)))) {
                ms.setAdapter(spinnerAdapter);
                ms.setSelection(1);
//                gender.setText("Male");
            } else {
                ms.setAdapter(spinnerAdapter);
                ms.setSelection(2);
//                gender.setText("Female");
            }

            placeList = mydb.getAllPlaces();
            for(int i = 0 ; i<placeList.size();i++){
                if(placeList.get(i).getId().equals("-100")){
                    notApplicablePos = i;
                    break;
                }
            }

            place_names = new ArrayList<String>();
            for (Place element : placeList) {
                place_names.add(element.getName());
            }
            place_names.add("--------");

            birthplaceList = mydb.getAllBirthplaces();
            List<String> birthplaceNames = new ArrayList<String>();
            for (Birthplace element : birthplaceList) {
                birthplaceNames.add(element.getName());
            }
            birthplaceNames.add("--------");

            SingleTextViewAdapter birthplaceAdapter = new SingleTextViewAdapter(ChildSummaryPagerFragment.this.getActivity(), R.layout.single_text_spinner_item_drop_down, birthplaceNames);
            pobSpinner.setAdapter(birthplaceAdapter);
            pobSpinner.setEnabled(false);

            int pos = birthplaceAdapter.getPosition(currentChild.getBirthplace());
            if (pos != -1) {
                pobSpinner.setSelection(pos+1);
                birthplaceOrig = pos;
            } else {
                pobSpinner.setSelection(birthplaceAdapter.getCount() - 1);
                birthplaceOrig = birthplaceAdapter.getCount() - 1;
            }

            SingleTextViewAdapter dataAdapter = new SingleTextViewAdapter(ChildSummaryPagerFragment.this.getActivity(), R.layout.single_text_spinner_item_drop_down, place_names);
            //@Teodor -> Modification -> E njejta liste si per Place of Birth dhe per Village
            villageSpinner.setAdapter(dataAdapter);
            villageSpinner.setEnabled(false);
            pos = place_names.indexOf(currentChild.getDomicile())+1;
            if (pos != -1) {
                villageSpinner.setSelection(pos);
                villageOrig = pos;
            } else {
                villageSpinner.setSelection(dataAdapter.getCount() - 1);
                villageOrig = dataAdapter.getCount() - 1;
            }

            healthFacilityList = mydb.getAllHealthFacility();
            List<String> facility_name = new ArrayList<String>();
            for (HealthFacility element : healthFacilityList) {
                facility_name.add(element.getName());
            }
            facility_name.add("------");

            SingleTextViewAdapter healthAdapter = new SingleTextViewAdapter(ChildSummaryPagerFragment.this.getActivity(), R.layout.single_text_spinner_item_drop_down, facility_name);
            healthFacilitySpinner.setAdapter(healthAdapter);
            healthFacilitySpinner.setEnabled(false);

            int index =facility_name.indexOf(currentChild.getHealthcenter());
            if (index != -1) {
                healthFacilitySpinner.setSelection(index+1);
                healthFacOrig = index+1;

            } else {
                healthFacilitySpinner.setSelection(healthAdapter.getCount()-1);
                healthFacOrig = healthAdapter.getCount()-1;
            }


            statusList = mydb.getStatus();
            List<String> status_name = new ArrayList<String>();

            for (Status element : statusList) {
                Log.d("Added status", element.getName());
                status_name.add(element.getName());
            }
            status_name.add("");


            SingleTextViewAdapter statusAdapter = new SingleTextViewAdapter(ChildSummaryPagerFragment.this.getActivity(), R.layout.single_text_spinner_item_drop_down, status_name);
            statusSpinner.setAdapter(statusAdapter);
            statusSpinner.setEnabled(false);
            pos = statusAdapter.getPosition(currentChild.getStatus());


            //TODO: Check at what time is the Status of A child inserted because in the
            //TODO: current version it is not captured during registering the child

            if (pos != -1) {
                statusSpinner.setSelection(pos+1);
                statusOrig = pos;
            } else {
                statusSpinner.setSelection(statusAdapter.getCount() - 1);
                statusOrig = statusAdapter.getCount() - 1;
            }

        }
    }

    private void initListeners() {
        pobSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                pobSpinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //no changes
            }

        });
//        birthplace.setEnabled(true);

        villageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                villageSpinner.setSelection(position);
                Log.d("coze",place_names.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //no changes
            }

        });
//        village.setEnabled(true);

        healthFacilitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                healthFacilitySpinner.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //no changes
            }

        });
//        healthFacility.setEnabled(true);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                statusSpinner.setSelection(position);
                // check if status is not active, if so than block everything else for being editable
                if (statusSpinner.getSelectedItemPosition() != 2) {
                    enableUserInputs(false);
                } else {
                    enableUserInputs(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //no changes
            }

        });
//        status.setEnabled(true);

        //if the child have done vacinations in the past we can not anymore change birthday

//        weight.setOnClickListener(this);
//        aefi.setOnClickListener(this);
//        immunization_card.setOnClickListener(this);
//        save.setOnClickListener(this);
    }

    public Child getChildFromCursror(Cursor cursor) {
        Child parsedChild = new Child();
        parsedChild.setId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.ID)));
        parsedChild.setBarcodeID(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.BARCODE_ID)));
        parsedChild.setTempId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.TEMP_ID)));
        parsedChild.setFirstname1(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME1)));
        parsedChild.setFirstname2(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.FIRSTNAME2)));
        parsedChild.setLastname1(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.LASTNAME1)));
        parsedChild.setBirthdate(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.BIRTHDATE)));
        parsedChild.setMotherFirstname(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_FIRSTNAME)));
        parsedChild.setMotherLastname(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.MOTHER_LASTNAME)));
        parsedChild.setPhone(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.PHONE)));
        parsedChild.setNotes(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.NOTES)));
        parsedChild.setBirthplaceId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.BIRTHPLACE_ID)));
        parsedChild.setGender(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.GENDER)));
        Cursor cursor1 = mydb.getReadableDatabase().rawQuery("SELECT * FROM birthplace WHERE ID=?", new String[]{parsedChild.getBirthplaceId()});
        if (cursor1.getCount() > 0) {
            cursor1.moveToFirst();
            birthplacestr = cursor1.getString(cursor1.getColumnIndex(SQLHandler.PlaceColumns.NAME));
        }
        parsedChild.setBirthplace(birthplacestr);

        parsedChild.setDomicileId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.DOMICILE_ID)));
        Cursor cursor2 = mydb.getReadableDatabase().rawQuery("SELECT * FROM place WHERE ID=?", new String[]{parsedChild.getDomicileId()});
        if (cursor2.getCount() > 0) {
            cursor2.moveToFirst();
            villagestr = cursor2.getString(cursor2.getColumnIndex(SQLHandler.PlaceColumns.NAME));
        }

        parsedChild.setDomicile(villagestr);
        parsedChild.setHealthcenterId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.HEALTH_FACILITY_ID)));
        try {
            Cursor cursor3 = mydb.getReadableDatabase().rawQuery("SELECT * FROM health_facility WHERE ID=?", new String[]{parsedChild.getHealthcenterId()});
            if (cursor3.getCount() > 0) {
                cursor3.moveToFirst();
                hfstr = cursor3.getString(cursor3.getColumnIndex(SQLHandler.HealthFacilityColumns.NAME));
            }
        }catch (Exception e){
            hfstr = "";
        }
        parsedChild.setHealthcenter(hfstr);

        parsedChild.setStatusId(cursor.getString(cursor.getColumnIndex(SQLHandler.ChildColumns.STATUS_ID)));
        Cursor cursor4 = mydb.getReadableDatabase().rawQuery("SELECT * FROM status WHERE ID=?", new String[]{parsedChild.getStatusId()});
        if (cursor4.getCount() > 0) {
            cursor4.moveToFirst();
            statusstr = cursor4.getString(cursor4.getColumnIndex(SQLHandler.StatusColumns.NAME));
        }
        parsedChild.setStatus(statusstr);
        return parsedChild;

    }

    /**
     * This funcition is for checking if the data that we are trying to update for the child are
     * accepptable.
     */
    private boolean checkDataIntegrityBeforeSave() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChildSummaryPagerFragment.this.getActivity())
                .setTitle(getString(R.string.alert_empty_fields))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((AlertDialog) dialog).dismiss();
                    }
                });
        if (metBarcodeValue.getText().toString().isEmpty()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_barcode));
            alertDialogBuilder.show();
            return false;
        }
        if (!metBarcodeValue.getText().toString().equalsIgnoreCase(currentChild.getBarcodeID())) {
            if (mydb.isBarcodeInChildTable(metBarcodeValue.getText().toString())) {
                alertDialogBuilder.setMessage(getString(R.string.barcode_assigned));
                alertDialogBuilder.show();
                return false;
            }
        }
        if (metFirstName.getText().toString().isEmpty() || metFirstName.getText().toString().isEmpty()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_names));
            alertDialogBuilder.show();
            return false;
        }
        if (metMothersFirstName.getText().toString().isEmpty() || metMothersFirstName.getText().toString().isEmpty()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_mother_names));
            alertDialogBuilder.show();
            return false;
        }
        if (metMothersFirstName.getText().toString().isEmpty() || metMothersFirstName.getText().toString().isEmpty()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_mother_names));
            alertDialogBuilder.show();
            return false;
        }
        if (bdate.compareTo(new Date()) > 0) {
            alertDialogBuilder.setMessage(getString(R.string.future_birth_date));
            alertDialogBuilder.show();
            return false;
        }
        // we have as the last element the one that is empty element. We can not select it.
        if (pobSpinner.getSelectedItemPosition() == birthplaceList.size()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_birthplace));
            alertDialogBuilder.show();
            return false;
        }
        // we have as the last element the one that is empty element. We can not select it.
        if (villageSpinner.getSelectedItemPosition() == placeList.size()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_village));
            alertDialogBuilder.show();
            return false;
        }
        if (healthFacilitySpinner.getSelectedItemPosition() == healthFacilityList.size()+1) {
            alertDialogBuilder.setMessage(getString(R.string.empty_healthfacility));
            alertDialogBuilder.show();
            return false;
        }
        if (statusSpinner.getSelectedItemPosition() == statusList.size()) {
            alertDialogBuilder.setMessage(getString(R.string.empty_status));
            alertDialogBuilder.show();
            return false;
        }

        return true;
    }

    private void showAlertThatChildHadABarcode() {
        ChildDetailsActivity parent = (ChildDetailsActivity)ChildSummaryPagerFragment.this.getActivity();
        final AlertDialog.Builder ad = new AlertDialog.Builder(ChildSummaryPagerFragment.this.getActivity());

        ad.setTitle(getString(R.string.warning));
        ad.setMessage(getString(R.string.barcode_already_entered));
        ad.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        ad.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveChangedData();
                enableUserInputs(false);
                dialog.dismiss();
            }
        });

        // this will solve your error
        AlertDialog alert = ad.create();
        alert.show();
        alert.getWindow().getAttributes();

        TextView textView = (TextView) alert.findViewById(android.R.id.message);
        textView.setTextSize(30);
    }

    /**
     * This is a method that is used to check if the user has changed any data from the data of the child
     * If yes that we save the changes, if not then we toast this.
     */
    private void saveChangedData() {
        giveValueAfterSave();
        ContentValues contentValues = new ContentValues();

        if (!metBarcodeValue.getText().toString().equalsIgnoreCase(currentChild.getBarcodeID())) {
            currentChild.setBarcodeID(metBarcodeValue.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.BARCODE_ID, metBarcodeValue.getText().toString());
        }
        if (!metFirstName.getText().toString().equalsIgnoreCase(currentChild.getFirstname1())) {
            currentChild.setFirstname1(metFirstName.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.FIRSTNAME1, metFirstName.getText().toString());
        }
        if (!metNotesValue.getText().toString().equalsIgnoreCase(currentChild.getNotes())) {
            currentChild.setNotes(metNotesValue.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.NOTES, metNotesValue.getText().toString());
        }
        if (!metMiddleName.getText().toString().equalsIgnoreCase(currentChild.getFirstname2())) {
            currentChild.setFirstname2(metMiddleName.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.FIRSTNAME2, metMiddleName.getText().toString());
        }
        if (!metLastName.getText().toString().equalsIgnoreCase(currentChild.getLastname1())) {
            currentChild.setLastname1(metLastName.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.LASTNAME1, metLastName.getText().toString());
        }
        if (bdate.compareTo(BackboneActivity.dateParser(currentChild.getBirthdate())) != 0) {
            birthDatesDiff = bdate.getTime() - BackboneActivity.dateParser(currentChild.getBirthdate()).getTime();
            // trick qe te marrim sa dite diference kemi dhe te gjejme fiks me sa dite ndryshon datelindja ne terma timestamp
            // e bejme gjithashtu nje floor te divisionit keshtu qe marrim vetem pjesen e plote te pjestimit dhe nuk
            // ngaterrohemi me castimin ne int. Ne cdo rast duhet te kemi kujdes ne mos numrat na kastohen ne int per
            // arsye se int do te na japi nje overflow dhe si pasoje nuk do te na ktheje vleren e sakte.
            // tani nuk do te na duhet me qe te bejme trickun me kalimin e nje dite diference pasi ate e kemi pasur si problem nga
            // overflow qe na bente int.
            double daysDiff = Math.floor(birthDatesDiff / 86400000);
            birthDatesDiff = (long) daysDiff * 86400000;
            currentChild.setBirthdate(birthdate_val);
            contentValues.put(SQLHandler.ChildColumns.BIRTHDATE, BackboneActivity.stringToDateParser(bdate));
        }

        if (!metMothersFirstName.getText().toString().equalsIgnoreCase(currentChild.getMotherFirstname())) {
            currentChild.setMotherFirstname(metMothersFirstName.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.MOTHER_FIRSTNAME, metMothersFirstName.getText().toString());
        }
        if (!metMothersSurname.getText().toString().equalsIgnoreCase(currentChild.getMotherLastname())) {
            currentChild.setMotherLastname(metMothersSurname.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.MOTHER_LASTNAME, metMothersSurname.getText().toString());
        }
        if (!birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getName().equalsIgnoreCase(currentChild.getBirthplace())) {
            currentChild.setBirthplaceId(birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getId());
            currentChild.setBirthplace(birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.BIRTHPLACE, birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.BIRTHPLACE_ID, birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getId());
        }
        Log.d("coze","vilage name = "+placeList.get(villageSpinner.getSelectedItemPosition()-1).getName());
        if (!placeList.get(villageSpinner.getSelectedItemPosition()-1).getName().equalsIgnoreCase(currentChild.getDomicile())) {
            currentChild.setDomicileId(placeList.get(villageSpinner.getSelectedItemPosition()-1).getId());
            currentChild.setDomicile(placeList.get(villageSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.DOMICILE, placeList.get(villageSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.DOMICILE_ID, placeList.get(villageSpinner.getSelectedItemPosition()-1).getId());
        }
        if (!healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getName().equalsIgnoreCase(currentChild.getHealthcenter())) {
            currentChild.setHealthcenterId(healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getId());
            currentChild.setHealthcenter(healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.HEALTH_FACILITY, healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.HEALTH_FACILITY_ID, healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getId());
        }
        if (!statusList.get(statusSpinner.getSelectedItemPosition()-1).getName().equalsIgnoreCase(currentChild.getStatus())) {
            currentChild.setStatusId(statusList.get(statusSpinner.getSelectedItemPosition()-1).getId());
            currentChild.setStatus(statusList.get(statusSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.STATUS, statusList.get(statusSpinner.getSelectedItemPosition()-1).getName());
            contentValues.put(SQLHandler.ChildColumns.STATUS_ID, statusList.get(statusSpinner.getSelectedItemPosition()-1).getId());
        }

//        if (male.isChecked() && !gender_val.equalsIgnoreCase("male")) {
//            contentValues.put(SQLHandler.ChildColumns.GENDER, "true");
//        } else if (female.isChecked() && !gender_val.equalsIgnoreCase("female")) {
//            contentValues.put(SQLHandler.ChildColumns.GENDER, "false");
//        }

        if (!metPhoneNumber.getText().toString().equalsIgnoreCase(currentChild.getPhone())) {
            currentChild.setPhone(metPhoneNumber.getText().toString());
            contentValues.put(SQLHandler.ChildColumns.PHONE, currentChild.getPhone());
        }
//        if (!notes.getText().toString().equalsIgnoreCase(currentChild.getNotes())) {
//            currentChild.setNotes(notes.getText().toString());
//            contentValues.put(SQLHandler.ChildColumns.NOTES, currentChild.getNotes());
//        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChildSummaryPagerFragment.this.getActivity())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ((AlertDialog) dialog).dismiss();
                    }
                });

        try {
            if (contentValues.size() > 0) {

                if (mydb.updateChild(contentValues, currentChild.getId()) > 0) {
                    if (birthDatesDiff != 0) {
                        mydb.updateVaccinationAppointementForBirthDtChangeChild(currentChild.getId(), birthDatesDiff);
                        mydb.updateVaccinationEventForBirthDtChangeChild(currentChild.getId(), birthDatesDiff);
                        loadViewAppointementsTable(true);
                    }

                    // bejme update statusin e appointement nese ka ndryshuar statusi i childit
                    if (!currentChild.getStatusId().equalsIgnoreCase("1"))
                        mydb.updateVaccinationAppointementDisactive(currentChild.getId());
                    // bejme update vacination appointement nese
                    if (contentValues.get(SQLHandler.ChildColumns.HEALTH_FACILITY_ID) != null) {
                        mydb.updateVaccinationAppointementNewFacility(currentChild.getId(), currentChild.getHealthcenterId());
                        mydb.updateVaccinationEventNewFacility(currentChild.getId(), currentChild.getHealthcenterId());
                    }

                    alertDialogBuilder.setMessage(R.string.child_change_data_saved_success);
                    thread = new Thread() {
                        @Override
                        public void run() {
                            String url = prepareUrl().toString();
                            String threadTodayTimestamp= null;
                            BackboneApplication backbone = (BackboneApplication) ChildSummaryPagerFragment.this.getActivity().getApplication();
                            if (!app.updateChild(prepareUrl())) {
                                mydb.addPost(url, -1);
                                Log.d("Save Edited Child", "Error while saving edited child " + currentChild.getId());
                                app.saveNeeded = false;
                            } else {
                                String dateTodayTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(Calendar.getInstance().getTime());
                                try {
                                    threadTodayTimestamp = URLEncoder.encode(dateTodayTimestamp, "utf-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                //Register Audit
                                try{
                                    backbone.registerAudit(BackboneApplication.CHILD_AUDIT, metBarcodeValue.getText().toString(), threadTodayTimestamp ,
                                            backbone.getLOGGED_IN_USER_ID(), 2);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                app.saveNeeded = false;
                            }
                        }
                    };
                    thread.start();
                } else {
                    alertDialogBuilder.setMessage(R.string.child_change_data_saved_error);
                    initListeners();
                }
                alertDialogBuilder.show();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(ChildSummaryPagerFragment.this.getActivity(), "Save failed", Toast.LENGTH_SHORT).show();
            app.saveNeeded = false;
            enableUserInputs(false);
        }


    }

    private StringBuilder prepareUrl() {
        final StringBuilder webServiceUrl = new StringBuilder(BackboneApplication.WCF_URL)
                .append(BackboneApplication.CHILD_MANAGEMENT_SVC).append(BackboneApplication.CHILD_UPDATE);
        try {
            webServiceUrl.append("barcode=" + URLEncoder.encode(metBarcodeValue.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&firstname1=" + URLEncoder.encode(metFirstName.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&notes=" + URLEncoder.encode(metNotesValue.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&lastname1=" + URLEncoder.encode(metLastName.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SimpleDateFormat formatted = new SimpleDateFormat("yyyy-MM-dd");
        try {
            webServiceUrl.append("&birthdate=" + URLEncoder.encode(formatted.format(bdate), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&motherFirstname=" + URLEncoder.encode(metMothersFirstName.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&motherLastname=" + URLEncoder.encode(metMothersSurname.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&birthplaceId=" + URLEncoder.encode(birthplaceList.get(pobSpinner.getSelectedItemPosition()-1).getId(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&domicileId=" + URLEncoder.encode(placeList.get(villageSpinner.getSelectedItemPosition()-1).getId(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&healthFacilityId=" + URLEncoder.encode(healthFacilityList.get(healthFacilitySpinner.getSelectedItemPosition()-1).getId(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&statusid=" + URLEncoder.encode(statusList.get(statusSpinner.getSelectedItemPosition()-1).getId(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (ms.getSelectedItemPosition() == 0)
            webServiceUrl.append("&gender=true");
        else if (ms.getSelectedItemPosition() == 1){
            webServiceUrl.append("&gender=true");
        }

        try {
            webServiceUrl.append("&phone=" + URLEncoder.encode(metPhoneNumber.getText().toString(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            webServiceUrl.append("&notes=" + URLEncoder.encode(notesOrig, "UTF-8"));
            if (currentChild.getId().matches("\\d+")) {
                webServiceUrl.append("&childId=" + URLEncoder.encode(currentChild.getId(), "UTF-8"));
            } else {
                webServiceUrl.append("&childId=" + 0); // hardcoded workaround for issues related to guid
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            webServiceUrl.append("&firstname2=" + URLEncoder.encode(metMiddleName.getText().toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            Log.d("coze", "updating child modified on = " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(Calendar.getInstance().getTime()));
            webServiceUrl.append("&modifiedOn=" + URLEncoder.encode(new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(Calendar.getInstance().getTime()), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return webServiceUrl;
    }

    public void giveValueAfterSave(){
        tempIdOrig = metSystemID.getText().toString();
        firstnameOrig = metFirstName.getText().toString();
        firstname2Orig = metMiddleName.getText().toString();
        lastnameOrig = metLastName.getText().toString();
        birthdateOrig = metDOB.getText().toString();
        motherFirOrig = metMothersFirstName.getText().toString();
        motherLastOrig = metMothersSurname.getText().toString();
        phoneOrig = metPhoneNumber.getText().toString();
        notesOrig = currentChild.getNotes();
        barcodeOrig = metBarcodeValue.getText().toString();
        birthplaceOrig = pobSpinner.getSelectedItemPosition();
        villageOrig = villageSpinner.getSelectedItemPosition();
        healthFacOrig = healthFacilitySpinner.getSelectedItemPosition();
        statusOrig = statusSpinner.getSelectedItemPosition();
        genderOrig =   ms.getSelectedItemPosition();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}