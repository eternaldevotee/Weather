import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.border.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class WeatherApp extends JFrame {
    private JTextField searchField;
    private JPanel weatherPanel;
    private JPanel forecastPanel;
    private JPanel favoritesPanel;
    private JLabel statusLabel;
    private JToggleButton unitToggle;
    private boolean isCelsius = true;
    private List<String> favorites;
    private Map<String, WeatherData> cachedWeatherData;
    private static final String API_KEY = "your_api_key_here"; // Replace with your API key
    private static final String FAVORITES_FILE = "favorites.txt";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new WeatherApp();
        });
    }
    
    public WeatherApp() {
        favorites = new ArrayList<>();
        cachedWeatherData = new HashMap<>();
        loadFavorites();
        
        setTitle("Weather Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        weatherPanel = new JPanel();
        weatherPanel.setLayout(new BoxLayout(weatherPanel, BoxLayout.Y_AXIS));
        weatherPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        forecastPanel = new JPanel();
        forecastPanel.setLayout(new BoxLayout(forecastPanel, BoxLayout.X_AXIS));
        forecastPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        
        mainPanel.add(weatherPanel, BorderLayout.CENTER);
        mainPanel.add(forecastPanel, BorderLayout.SOUTH);
        
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setBorder(null);
        
        favoritesPanel = new JPanel();
        favoritesPanel.setLayout(new BoxLayout(favoritesPanel, BoxLayout.Y_AXIS));
        favoritesPanel.setBorder(BorderFactory.createTitledBorder("Favorites"));
        updateFavoritesPanel();
        
        JScrollPane favoritesScrollPane = new JScrollPane(favoritesPanel);
        favoritesScrollPane.setPreferredSize(new Dimension(200, getHeight()));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                             favoritesScrollPane, mainScrollPane);
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);
        
        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.SOUTH);
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.addActionListener(e -> searchWeather());
        
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchWeather());
        
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        unitToggle = new JToggleButton("°C");
        unitToggle.addActionListener(e -> {
            isCelsius = !isCelsius;
            unitToggle.setText(isCelsius ? "°C" : "°F");
            if (!searchField.getText().trim().isEmpty()) {
                updateWeatherDisplay(searchField.getText().trim());
            }
        });
        
        JButton addFavoriteButton = new JButton("Add to Favorites");
        addFavoriteButton.addActionListener(e -> {
            String city = searchField.getText().trim();
            if (!city.isEmpty() && !favorites.contains(city)) {
                favorites.add(city);
                saveFavorites();
                updateFavoritesPanel();
            }
        });
        
        controlPanel.add(addFavoriteButton);
        controlPanel.add(unitToggle);
        
        panel.add(searchPanel, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void searchWeather() {
        String city = searchField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a city name", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        statusLabel.setText("Fetching weather data for " + city + "...");
        
        SwingWorker<WeatherData, Void> worker = new SwingWorker<>() {
            @Override
            protected WeatherData doInBackground() {
                try {
                    if (cachedWeatherData.containsKey(city) && 
                        System.currentTimeMillis() - cachedWeatherData.get(city).timestamp < 3600000) {
                        return cachedWeatherData.get(city);
                    }
                    
                    URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + 
                                     URLEncoder.encode(city, "UTF-8") + 
                                     "&appid=" + API_KEY + 
                                     "&units=metric");
                    
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    // For a real app, parse JSON properly using a library like Gson or Jackson
                    // This is a simplified example
                    String jsonResponse = response.toString();
                    
                    // Simulate parsing JSON
                    WeatherData data = new WeatherData();
                    data.city = city;
                    data.temperature = 22.5; // Simulated temperature in Celsius
                    data.humidity = 65;
                    data.windSpeed = 5.2;
                    data.description = "Partly Cloudy";
                    data.iconCode = "02d";
                    data.timestamp = System.currentTimeMillis();
                    
                    // Simulate forecast data
                    data.forecast = new ArrayList<>();
                    for (int i = 1; i <= 5; i++) {
                        ForecastDay day = new ForecastDay();
                        day.date = new Date(System.currentTimeMillis() + i * 86400000);
                        day.highTemp = 22.0 + i;
                        day.lowTemp = 15.0 + i;
                        day.iconCode = "01d";
                        data.forecast.add(day);
                    }
                    
                    cachedWeatherData.put(city, data);
                    return data;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            
            @Override
            protected void done() {
                try {
                    WeatherData data = get();
                    if (data != null) {
                        updateWeatherDisplay(data);
                        statusLabel.setText("Weather data updated for " + city);
                    } else {
                        statusLabel.setText("Error fetching weather data for " + city);
                        JOptionPane.showMessageDialog(WeatherApp.this, 
                                                     "Could not fetch weather data. Please check the city name and try again.",
                                                     "Error", 
                                                     JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    statusLabel.setText("Error: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }
    
    private void updateWeatherDisplay(String city) {
        if (cachedWeatherData.containsKey(city)) {
            updateWeatherDisplay(cachedWeatherData.get(city));
        } else {
            searchField.setText(city);
            searchWeather();
        }
    }
    
    private void updateWeatherDisplay(WeatherData data) {
        weatherPanel.removeAll();
        forecastPanel.removeAll();
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel cityLabel = new JLabel(data.city);
        cityLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(cityLabel, BorderLayout.WEST);
        
        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        headerPanel.add(dateLabel, BorderLayout.EAST);
        
        weatherPanel.add(headerPanel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel currentWeatherPanel = new JPanel(new BorderLayout());
        
        JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        double displayTemp = isCelsius ? data.temperature : celsiusToFahrenheit(data.temperature);
        JLabel tempLabel = new JLabel(String.format("%.1f°%s", displayTemp, isCelsius ? "C" : "F"));
        tempLabel.setFont(new Font("Arial", Font.BOLD, 48));
        tempPanel.add(tempLabel);
        
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.add(new JLabel("Humidity: " + data.humidity + "%"));
        detailsPanel.add(new JLabel("Wind: " + data.windSpeed + " m/s"));
        detailsPanel.add(new JLabel(data.description));
        
        currentWeatherPanel.add(tempPanel, BorderLayout.WEST);
        currentWeatherPanel.add(detailsPanel, BorderLayout.CENTER);
        
        weatherPanel.add(currentWeatherPanel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        JLabel forecastLabel = new JLabel("5-Day Forecast");
        forecastLabel.setFont(new Font("Arial", Font.BOLD, 18));
        weatherPanel.add(forecastLabel);
        weatherPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        for (ForecastDay day : data.forecast) {
            JPanel dayPanel = new JPanel();
            dayPanel.setLayout(new BoxLayout(dayPanel, BoxLayout.Y_AXIS));
            dayPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY), 
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            
            JLabel dayLabel = new JLabel(new SimpleDateFormat("EEE").format(day.date));
            dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel iconLabel = new JLabel("☀"); // Simplified icon
            iconLabel.setFont(new Font("Arial", Font.PLAIN, 24));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            double highTemp = isCelsius ? day.highTemp : celsiusToFahrenheit(day.highTemp);
            double lowTemp = isCelsius ? day.lowTemp : celsiusToFahrenheit(day.lowTemp);
            
            JLabel highLabel = new JLabel(String.format("%.1f°", highTemp));
            highLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            JLabel lowLabel = new JLabel(String.format("%.1f°", lowTemp));
            lowLabel.setForeground(Color.GRAY);
            lowLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            dayPanel.add(dayLabel);
            dayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            dayPanel.add(iconLabel);
            dayPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            dayPanel.add(highLabel);
            dayPanel.add(lowLabel);
            
            dayPanel.setPreferredSize(new Dimension(100, 120));
            dayPanel.setMaximumSize(new Dimension(100, 120));
            
            forecastPanel.add(dayPanel);
            forecastPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        }
        
        weatherPanel.revalidate();
        weatherPanel.repaint();
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }
    
    private double celsiusToFahrenheit(double celsius) {
        return celsius * 9 / 5 + 32;
    }
    
    private void updateFavoritesPanel() {
        favoritesPanel.removeAll();
        
        for (String city : favorites) {
            JPanel cityPanel = new JPanel(new BorderLayout());
            JLabel cityLabel = new JLabel(city);
            cityLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            JButton removeButton = new JButton("×");
            removeButton.setPreferredSize(new Dimension(20, 20));
            removeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            removeButton.addActionListener(e -> {
                favorites.remove(city);
                saveFavorites();
                updateFavoritesPanel();
            });
            
            cityPanel.add(cityLabel, BorderLayout.CENTER);
            cityPanel.add(removeButton, BorderLayout.EAST);
            cityPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            
            cityPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    searchField.setText(city);
                    updateWeatherDisplay(city);
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    cityPanel.setBackground(new Color(230, 230, 230));
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    cityPanel.setBackground(null);
                }
            });
            
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.add(cityPanel, BorderLayout.NORTH);
            favoritesPanel.add(wrapperPanel);
        }
        
        favoritesPanel.revalidate();
        favoritesPanel.repaint();
    }
    
    private void loadFavorites() {
        try {
            File file = new File(FAVORITES_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    favorites.add(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveFavorites() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(FAVORITES_FILE));
            for (String city : favorites) {
                writer.write(city);
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static class WeatherData {
        String city;
        double temperature;
        int humidity;
        double windSpeed;
        String description;
        String iconCode;
        long timestamp;
        List<ForecastDay> forecast;
    }
    
    private static class ForecastDay {
        Date date;
        double highTemp;
        double lowTemp;
        String iconCode;
    }
}
