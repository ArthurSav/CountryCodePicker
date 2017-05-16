package com.sithagi.countrycodepicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class CountryPicker extends DialogFragment implements Comparator<Country> {

  public static final String EXTRA_COUNTRIES = "countries";
  public static final String EXTRA_TITLE = "title";
  private String[] displayedCountriesCodes;

  private EditText searchEditText;
  private ListView countryListView;

  private CountryListAdapter adapter;

  private List<Country> allCountriesList;
  private List<Country> selectedCountriesList;

  private CountryPickerListener listener;

  public void setListener(CountryPickerListener listener) {
    this.listener = listener;
  }

  public EditText getSearchEditText() {
    return searchEditText;
  }

  public ListView getCountryListView() {
    return countryListView;
  }

  public static Currency getCurrencyCode(String countryCode) {
    try {
      return Currency.getInstance(new Locale("en", countryCode));
    } catch (Exception e) {

    }
    return null;
  }

  private List<Country> getCountriesFromFile() {
    if (allCountriesList == null) {
      try {
        allCountriesList = new ArrayList<Country>();

        String allCountriesCode = readEncodedJsonString(getActivity());

        JSONArray countrArray = new JSONArray(allCountriesCode);

        for (int i = 0; i < countrArray.length(); i++) {
          JSONObject jsonObject = countrArray.getJSONObject(i);
          String countryName = jsonObject.getString("name");
          String countryDialCode = jsonObject.getString("dial_code");
          String countryCode = jsonObject.getString("code");

          Country country = new Country();
          country.setCode(countryCode);
          country.setName(countryName);
          country.setDialCode(countryDialCode);
          allCountriesList.add(country);
        }

        Collections.sort(allCountriesList, this);

        selectedCountriesList = new ArrayList<Country>();
        selectedCountriesList.addAll(allCountriesList);

        // Return
        return allCountriesList;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static String readEncodedJsonString(Context context) throws java.io.IOException {
    String base64 = context.getResources().getString(R.string.countries_code);
    byte[] data = Base64.decode(base64, Base64.DEFAULT);
    return new String(data, "UTF-8");
  }

  /**
   * To support show as dialog
   */
  public static CountryPicker newInstance(String title) {
    CountryPicker picker = new CountryPicker();
    Bundle bundle = new Bundle();
    bundle.putString(EXTRA_TITLE, title);
    picker.setArguments(bundle);
    return picker;
  }

  /**
   * Use this instance to restrict displated countries by providing the country code to be displayed
   * @param title dialog title
   * @param countries countries to show e.g Countries[GB, GR, ES]
   * @return dialog picker
   */
  public static CountryPicker newInstance(String title, String[] countries) {
    Bundle args = new Bundle();
    args.putStringArray(EXTRA_COUNTRIES, countries);
    args.putString(EXTRA_TITLE, title);
    CountryPicker fragment = new CountryPicker();
    fragment.setArguments(args);

    return fragment;
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.country_picker, null);

    getCountriesFromFile();

    Bundle args = getArguments();
    if (args != null) {
      String dialogTitle = args.getString(EXTRA_TITLE);
      getDialog().setTitle(dialogTitle);

      displayedCountriesCodes = args.getStringArray(EXTRA_COUNTRIES);

      int width = getResources().getDimensionPixelSize(R.dimen.cp_dialog_width);
      int height = getResources().getDimensionPixelSize(R.dimen.cp_dialog_height);
      getDialog().getWindow().setLayout(width, height);
    }

    if (displayedCountriesCodes != null) {
      List<Country> filteredCountries = getCountriesWithCodes(displayedCountriesCodes);
      this.allCountriesList = filteredCountries;
      this.selectedCountriesList = filteredCountries;
    }

    searchEditText = (EditText) view.findViewById(R.id.country_code_picker_search);
    countryListView = (ListView) view.findViewById(R.id.country_code_picker_listview);

    adapter = new CountryListAdapter(getActivity(), selectedCountriesList);
    countryListView.setAdapter(adapter);

    countryListView.setOnItemClickListener(new OnItemClickListener() {

      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (listener != null) {
          Country country = selectedCountriesList.get(position);
          listener.onSelectCountry(country.getName(), country.getCode(), country.getDialCode());
        }
      }
    });

    searchEditText.addTextChangedListener(new TextWatcher() {

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void afterTextChanged(Editable s) {
        search(s.toString());
      }
    });

    return view;
  }



  private List<Country> getCountriesWithCodes(String[] countryCodes){
    List<Country> filtered = new ArrayList<>();
    for (String code : countryCodes) {
      Country country = getCountryByCode(code);
      if (country != null) filtered.add(country);
    }
    return filtered;
  }

  private Country getCountryByCode(String countryCode){
    for (Country country : allCountriesList) {
      if (country.getCode().equalsIgnoreCase(countryCode)) {
        return country;
      }
    }
    return null;
  }

  @SuppressLint("DefaultLocale") private void search(String text) {
    selectedCountriesList.clear();

    for (Country country : allCountriesList) {
      if (country.getName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase())) {
        selectedCountriesList.add(country);
      }
    }

    adapter.notifyDataSetChanged();
  }

  @Override public int compare(Country lhs, Country rhs) {
    return lhs.getName().compareTo(rhs.getName());
  }
}
