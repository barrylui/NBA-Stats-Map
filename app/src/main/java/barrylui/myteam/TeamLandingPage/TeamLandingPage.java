package barrylui.myteam.TeamLandingPage;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;

import java.util.ArrayList;
import java.util.Arrays;

import barrylui.myteam.InternetUtilities.BasicAuthInterceptor;
import barrylui.myteam.ChooseYourTeam;
import barrylui.myteam.InternetUtilities.InternetCheckerUtility;
import barrylui.myteam.MySportsFeedAPI.MySportsFeedTeamRankingsModel.Standings;
import barrylui.myteam.R;
import barrylui.myteam.MySportsFeedAPI.MySportsFeedTeamRankingsModel.TeamRankingsModel.Rankings;
import barrylui.myteam.TeamRoster.RosterViewer;
import barrylui.myteam.MySportsFeedAPI.SportsFeedAPI;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class TeamLandingPage extends AppCompatActivity{


    TextView franchiseName;
    TextView winstv;
    TextView losestv;
    TextView dashtv;
    TextView ppgtv;
    TextView oppgtv;
    TextView apgtv;
    TextView rpgtv;
    TextView tpatv;
    TextView tpptv;
    TextView teamranktv;
    Button teamRoster;
    TextView infotv;
    RadarChart radarChart;

    String teamName="";
    int teamConference=-1;
    int teamcolors;

    String city;
    String name;
    String standingsRank;
    Double teamppg;
    Double oppPPG;
    Double teamapg;
    Double teamrpg;
    Double team3pt;

    int rankInside;
    int offenseRadarValue;
    int defenseRadarValue;
    int reboundsRadarValue;
    int passingRadarValue;
    int threesRadarValue;

    ArrayList<String> labels;

    final String BASE_URL = "https://api.mysportsfeeds.com/v1.2/pull/nba/2017-2018-regular/";
    OkHttpClient client;
    Retrofit retrofit;

    private static final String TAG = "TeamLandingPage";
    DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_landing_page);



        int teamid = getIntent().getIntExtra("TeamID",0);
        final int teamlogo = getIntent().getIntExtra("TeamLogo",0);
        teamcolors = getIntent().getIntExtra("TeamColors",0);
        teamName = getIntent().getStringExtra("TeamName");
        teamConference = getIntent().getIntExtra("TeamConference", -1);
        int teamInsideRank = getIntent().getIntExtra("PtsInPaintRank", -1);
        rankInside = 31-teamInsideRank;


        //Change status bar to match team color
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(teamcolors));


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(getDrawable(teamcolors));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        franchiseName = (TextView)findViewById(R.id.franchiseName);
        winstv  = (TextView)findViewById(R.id.numWinsTextView);
        losestv = (TextView)findViewById(R.id.numLosesTextView);
        dashtv = (TextView) findViewById(R.id.dashTextView);
        ppgtv = (TextView)findViewById(R.id.ppgtextview);
        oppgtv = (TextView)findViewById(R.id.oppgtextview);
        apgtv = (TextView)findViewById(R.id.apgtextview);
        rpgtv =(TextView)findViewById(R.id.rpgtextview);
        tpatv = (TextView)findViewById(R.id.textview3pm);
        teamranktv = (TextView)findViewById(R.id.teamStanding);
        infotv = (TextView)findViewById(R.id.infoTextView);
        teamRoster = (Button)findViewById(R.id.teamRosterButton) ;
        radarChart = (RadarChart)findViewById(R.id.radarchart);



        infotv.setTextColor(Color.WHITE);
        winstv.setTextColor(Color.WHITE);
        losestv.setTextColor(Color.WHITE);
        dashtv.setTextColor(Color.WHITE);
        teamranktv.setTextColor(Color.WHITE);
        ppgtv.setTextColor(Color.WHITE);
        oppgtv.setTextColor(Color.WHITE);
        apgtv.setTextColor(Color.WHITE);
        rpgtv.setTextColor(Color.WHITE);
        tpatv.setTextColor(Color.WHITE);

        if(teamName.equals("BRO")  || teamName.equals("SAS")){
            teamRoster.setTextColor(Color.BLACK);
            teamRoster.setBackgroundColor(Color.WHITE);
        }
        else{
            teamRoster.setBackgroundColor(getColor(teamcolors));
            teamRoster.setTextColor(Color.WHITE);

        }


        infotv.setText("Loading...");
        teamranktv.setText("Loading...");

        //Adjust imageview size for dallas because the dallas logo is not from nba.com
        ImageView iv = (ImageView)findViewById(R.id.teamlogo);
        iv.setImageResource(teamlogo);
        if(teamName.equals("DAL"))
        {
            iv.getLayoutParams().width = 550;
            iv.getLayoutParams().height = 550;

            iv.setScaleType(ImageView.ScaleType.FIT_XY);
        }



        client = new OkHttpClient.Builder()
                .addInterceptor(new BasicAuthInterceptor(getString(R.string.username), getString(R.string.password)))
                .build();

        String BASE_URL = "https://api.mysportsfeeds.com/v1.2/pull/nba/2017-2018-regular/";

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //Check if there is internet connection
        if(InternetCheckerUtility.isNetworkAvailable(TeamLandingPage.this)==false){
            //Display no internet connection and disable teamroster button;
            Toast.makeText(TeamLandingPage.this, "No Internet Connection", Toast.LENGTH_LONG).show();
            infotv.setText("No Internet Connection");
            teamRoster.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(TeamLandingPage.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            });
            //Disable team button
        }else{
            teamRoster.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent rosterIntent = new Intent(TeamLandingPage.this, RosterViewer.class);
                    rosterIntent.putExtra("TeamAbbrv", teamName);
                    rosterIntent.putExtra("TeamColor", teamcolors);
                    rosterIntent.putExtra("TeamLogo", teamlogo);
                    Log.d(TAG, "onClick: " + teamName);
                    startActivity(rosterIntent);
                }
            });
            new AsyncFetctTeamData().execute();

        }
    }

    private class AsyncFetctTeamData extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            getTeamStats();
            return null;
        }
    }

    public void getTeamStats(){
        SportsFeedAPI sportsFeedAPI = retrofit.create(SportsFeedAPI.class);
        String teamstatsparams = "W,L,PTS/G,AST/G,REB/G,3PA/G,3PM/G,PTS/G,PTSA/G";
        Call<Standings> call = sportsFeedAPI.getStandings(teamstatsparams, teamName);

        call.enqueue(new Callback<Standings>() {
            @Override
            public void onResponse(Call<Standings> call, Response<Standings> response) {
                Log.d(TAG, "onResponse: Server Response: " + response.toString());


                //Bind data if connection to endpoint is succcessful
                if(response.code()==200){
                    city = response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getTeam().getCity();
                    name = response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getTeam().getName();
                    franchiseName.setText(city + " " + name);

                    String numberOfWins = response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getWins().getText();
                    winstv.setText(numberOfWins);

                    String numberOfLoses = response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getLosses().getText();
                    losestv.setText(numberOfLoses);

                    teamppg = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getPtsPerGame().get(0).getText());

                    oppPPG = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getPtsAgainstPerGame().getText());

                    teamapg = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getAstPerGame().getText());

                    teamrpg = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getRebPerGame().getText());

                    team3pt = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getStats().getFg3PtMadePerGame().getText());

                    standingsRank = response.body().getConferenceteamstandings().getConference().get(teamConference).getTeamentry().get(0).getRank();
                    String conference;
                    if(teamConference == 0){
                        conference = "East";
                    }
                    else{
                        conference = "West";
                    }
                    standingsRank = "#" + standingsRank + " in the " + conference;
                    teamranktv.setText(standingsRank);

                    //Get data to bind to radarchart
                    getTeamStatsRankAndBindData();
                }
                else{
                    Log.d(TAG, "onResponse: Server Response " + response.toString());
                    Toast.makeText(TeamLandingPage.this, "Error " + response.toString(), Toast.LENGTH_LONG).show();
                }


            }

            @Override
            public void onFailure(Call<Standings> call, Throwable t) {
                Log.e(TAG, "onFailure: Something went wrong: " + t.getMessage());
                Toast.makeText(TeamLandingPage.this, "Something went wrong" + t.getMessage(), Toast.LENGTH_LONG).show();
                infotv.setText("No data available");
            }
        });
    }


    //Loads team's stats into an array and sorts the array
    //After sorting each team's stats, it can bind each stat to the chart based on their rank amongst other teams
    public void getTeamStatsRankAndBindData(){
        SportsFeedAPI sportsFeedAPI = retrofit.create(SportsFeedAPI.class);
        String params = "PTS/G,PTSA/G,REB/G,AST/G,3PM/G";
        Call<Rankings> call = sportsFeedAPI.getStatsRank(params);

        call.enqueue(new Callback<Rankings>() {
            @Override
            public void onResponse(Call<Rankings> call, Response<Rankings> response) {
                Log.d(TAG, "onResponse: Server Response: " + response.toString());

                if(response.code()==200){
                    //Arrays to sort stats to get rank for each stat
                    double[] rankOffense = new double[30];
                    double[] rankDefense = new double[30];
                    double[] rankRebound = new double[30];
                    double[] rankAssists = new double[30];
                    double[] rank3PTMade = new double[30];
                    int index = 0;

                    //Load in eastern conference team statistics into the array then the western conference teams
                    for(int i =0; i<2;i++){
                        for (int j= 0; j<15;j++){
                            rankOffense[index] = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(i).getTeamentry().get(j).getStats().getPtsPerGame().getText());
                            rankDefense[index] = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(i).getTeamentry().get(j).getStats().getPtsAgainstPerGame().getText());
                            rankRebound[index] = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(i).getTeamentry().get(j).getStats().getRebPerGame().getText());
                            rankAssists[index] = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(i).getTeamentry().get(j).getStats().getAstPerGame().getText());
                            rank3PTMade[index] = Double.parseDouble(response.body().getConferenceteamstandings().getConference().get(i).getTeamentry().get(j).getStats().getFg3PtMadePerGame().getText());
                            index++;
                        }
                    }

                    //sort array so team stats can be in order by rank
                    Arrays.sort(rankOffense);
                    Arrays.sort(rankDefense);
                    Arrays.sort(rankRebound);
                    Arrays.sort(rankAssists);
                    Arrays.sort(rank3PTMade);


                    //Search for selected teaM's stats and get the rank which is the index in array
                    for(int i=0; i<30; i++){
                        int offenseval = Double.compare(rankOffense[i],teamppg);
                        int defenseval = Double.compare(rankDefense[i],oppPPG);
                        int reboundval = Double.compare(rankRebound[i], teamrpg);
                        int passingval = Double.compare(rankAssists[i], teamapg);
                        int threesval = Double.compare(rank3PTMade[i], team3pt);

                        if (offenseval == 0){
                             offenseRadarValue = i;
                             int offensiveRank = 30 - offenseRadarValue;
                             ppgtv.setText(String.valueOf(offensiveRank));
                        }
                        if (defenseval == 0){
                            defenseRadarValue = i;
                            int defenseRank = defenseRadarValue;
                            oppgtv.setText(String.valueOf(defenseRank));
                        }
                        if (reboundval == 0){
                            reboundsRadarValue = i;
                            int reboundRank = 30 - reboundsRadarValue;
                            rpgtv.setText(String.valueOf(reboundRank));
                        }
                        if (passingval == 0){
                            passingRadarValue = i;
                            int assistRank = 30 - passingRadarValue;
                            apgtv.setText(String.valueOf(assistRank));
                        }
                        if (threesval == 0){
                            threesRadarValue = i;
                            int threesRank = 30 - threesRadarValue;
                            tpatv.setText(String.valueOf(threesRank));
                        }
                    }
                    int dRank = 31 - TeamLandingPage.this.defenseRadarValue;

                    //Load data into radar chart
                    ArrayList<Entry> entry1 = new ArrayList<>();
                    entry1.add(new Entry(reboundsRadarValue,0));
                    entry1.add(new Entry(TeamLandingPage.this.offenseRadarValue,1));
                    entry1.add(new Entry(dRank,2));
                    entry1.add(new Entry(passingRadarValue,3));
                    entry1.add(new Entry(rankInside,4));
                    entry1.add(new Entry(threesRadarValue,5));

                    //Label axis on radar chart
                    labels = new ArrayList<String>();
                    labels.add("Rebounds");
                    labels.add("Offense");
                    labels.add("Defense");
                    labels.add("Assists");
                    labels.add("Inside Scoring");
                    labels.add("3PT Scoring");

                    //Setup radarchart settings and bind radar chart
                    RadarDataSet dataset1 = new RadarDataSet(entry1,"Player");

                    if(teamName.equals("BRO") || teamName.equals("SAS")){
                        dataset1.setColor(Color.WHITE);
                        dataset1.setDrawFilled(true);
                        dataset1.setFillColor(Color.WHITE);
                    }
                    else{
                        dataset1.setColor(getColor(teamcolors));
                        dataset1.setDrawFilled(true);
                        dataset1.setFillColor(getColor(teamcolors));
                    }

                    dataset1.setFillAlpha(180);
                    dataset1.setLineWidth(5f);
                    dataset1.setDrawValues(false);

                    ArrayList<RadarDataSet> dataSets= new ArrayList<RadarDataSet>();
                    dataSets.add(dataset1);
                    RadarData theradardata = new RadarData(labels, dataSets);
                    radarChart.setData(theradardata);

                    //disable desciption
                    Legend l = radarChart.getLegend();
                    radarChart.setDescription("");
                    l.setEnabled(false);

                    radarChart.getXAxis().setTextColor(Color.WHITE);
                    //Max is 30 because there are 30 nba teams. Each stat is ranked against other teams and can be plotted to see how good/bad a team is in a paticular category
                    YAxis yAxis = radarChart.getYAxis();
                    yAxis.resetAxisMaxValue();
                    yAxis.setAxisMaxValue(30);
                    yAxis.setAxisMinValue(0);
                    yAxis.setDrawLabels(false);

                    radarChart.notifyDataSetChanged();
                    radarChart.invalidate();
                    infotv.setText("Team Profile");

                }
                else{
                    Log.d(TAG, "onResponse: Server Response " + response.toString());
                    Toast.makeText(TeamLandingPage.this, "Error " + response.toString(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Rankings> call, Throwable t) {
                Log.e(TAG, "onFailure: Something went wrong: " + t.getMessage());
                Toast.makeText(TeamLandingPage.this, "Something went wrong" + t.getMessage(), Toast.LENGTH_LONG).show();
                infotv.setText("No data available");
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ChooseYourTeam.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
