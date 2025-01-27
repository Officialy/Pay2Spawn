package net.doubledoordev.d3core.util.libs.org.mcstats;

import net.doubledoordev.d3core.D3Core;
import net.doubledoordev.oldforge.Configuration;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class Metrics
{

    /**
     * The current revision number
     */
    private final static int REVISION = 7;

    /**
     * The base url of the metrics domain
     */
    private static final String BASE_URL = "http://report.mcstats.org"; //todo this is down lol?

    /**
     * The url used to report a server's status
     */
    private static final String REPORT_URL = "/plugin/%s";

    /**
     * Interval of time to ping (in minutes)
     */
    private static final int PING_INTERVAL = 15;

    /**
     * The mod this metrics submits for
     */
    private final String modname;

    private final String modversion;

    /**
     * All of the custom graphs to submit to metrics
     */
    private final Set<Graph> graphs = Collections.synchronizedSet(new HashSet<Graph>());

    /**
     * The metrics configuration file
     */
    private final Configuration configuration;

    /**
     * The metrics configuration file
     */
    private final File configurationFile;

    /**
     * Unique server id
     */
    private final String guid;

    /**
     * Debug mode
     */
    private final boolean debug;

    public Metrics(final String modname, final String modversion) throws IOException
    {
        if ((modname == null) || (modversion == null))
        {
            throw new IllegalArgumentException("modname and modversion cannot be null");
        }

        this.modname = modname;
        this.modversion = modversion;

        // load the config
        configurationFile = getConfigFile();
        configuration = new Configuration(configurationFile);

        // Get values, and add some defaults, if needed
        configuration.get(Configuration.CATEGORY_GENERAL, "opt-out", false, "Set to true to disable all reporting");
        guid = configuration.get(Configuration.CATEGORY_GENERAL, "guid", UUID.randomUUID().toString(), "Server unique ID").getString();
        debug = configuration.get(Configuration.CATEGORY_GENERAL, "debug", false, "Set to true for verbose debug").getBoolean(false);
        configuration.save();
    }

    /**
     * Construct and create a Graph that can be used to separate specific
     * plotters to their own graphs on the metrics website. Plotters can be
     * added to the graph object returned.
     *
     * @param name The name of the graph
     * @return Graph object created. Will never return NULL under normal
     * circumstances unless bad parameters are given
     */
    public Graph createGraph(final String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Graph name cannot be null");
        }

        // Construct the graph object
        final Graph graph = new Graph(name);

        // Now we can add our graph
        graphs.add(graph);

        // and return back
        return graph;
    }

    /**
     * Add a Graph object to Metrics that represents data for the plugin that
     * should be sent to the backend
     *
     * @param graph The name of the graph
     */
    public void addGraph(final Graph graph)
    {
        if (graph == null)
        {
            throw new IllegalArgumentException("Graph cannot be null");
        }

        graphs.add(graph);
    }

    /**
     * Start measuring statistics. This will immediately create an async
     * repeating task as the plugin and send the initial data to the metrics
     * backend, and then after that it will post in increments of PING_INTERVAL
     * * 1200 ticks.
     *
     * @return True if statistics measuring is running, otherwise false.
     */
    public boolean start()
    {
        // Did we opt out?
        if (isOptOut())
        {
            return false;
        }

        MinecraftForge.EVENT_BUS.register(this);

        return true;
    }

    private Thread  thrd      = null;
    private boolean firstPost = true;
    int tickCount;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent tick)
    {
        if (tick.phase != TickEvent.Phase.END) return;

        // Disable Task, if it is running and the server owner decided
        // to opt-out
        if (isOptOut())
        {
            // Tell all plotters to stop gathering information.
            for (Graph graph : graphs)
            {
                graph.onOptOut();
            }

            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        }

        tickCount++;

        if (tickCount % (firstPost ? 100 : PING_INTERVAL * 1200) != 0) return;

        tickCount = 0;

        if (thrd == null)
        {
            thrd = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        // We use the inverse of firstPost because if it
                        // is the first time we are posting,
                        // it is not a interval ping, so it evaluates to
                        // FALSE
                        // Each time thereafter it will evaluate to
                        // TRUE, i.e PING!
                        postPlugin(!firstPost);
                        // After the first post we set firstPost to
                        // false
                        // Each post thereafter will be a ping
                        firstPost = false;
                    }
                    catch (IOException e)
                    {
                        if (debug)
                        {
                            D3Core.getLogger().info("[Metrics] Exception - %s", e.getMessage());
                        }
                    }
                    finally
                    {
                        thrd = null;
                    }
                }
            });
            thrd.start();
        }
    }

    /**
     * Stop processing
     */
    public void stop()
    {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    /**
     * Has the server owner denied plugin metrics?
     *
     * @return true if metrics should be opted out of it
     */
    public boolean isOptOut()
    {
        // Reload the metrics file
        configuration.load();
        return configuration.get(Configuration.CATEGORY_GENERAL, "opt-out", false).getBoolean(false);
    }

    /**
     * Enables metrics for the server by setting "opt-out" to false in the
     * config file and starting the metrics task.
     *
     * @throws java.io.IOException
     */
    public void enable() throws IOException
    {
        // Check if the server owner has already set opt-out, if not, set it.
        if (isOptOut())
        {
            configuration.getCategory(Configuration.CATEGORY_GENERAL).get("opt-out").set("false");
            configuration.save();
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Disables metrics for the server by setting "opt-out" to true in the
     * config file and canceling the metrics task.
     *
     * @throws java.io.IOException
     */
    public void disable() throws IOException
    {
        // Check if the server owner has already set opt-out, if not, set it.
        if (!isOptOut())
        {
            configuration.getCategory(Configuration.CATEGORY_GENERAL).get("opt-out").set("true");
            configuration.save();
        }
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    /**
     * Gets the File object of the config file that should be used to store data
     * such as the GUID and opt-out status
     *
     * @return the File object for the config file
     */
    public File getConfigFile()
    {
        return new File(FMLLoader.getGamePath()+"/"+"config"+"/", "PluginMetrics.cfg");
    }

    /**
     * Generic method that posts a plugin to the metrics website
     */
    private void postPlugin(final boolean isPing) throws IOException
    {
        // Server software specific section
        String pluginName = modname;
        boolean onlineMode = true;//todo MinecraftServer.getServer().isServerInOnlineMode();
        String pluginVersion = modversion;
        String serverVersion = "MinecraftForge idkwhattoputherern";
       /* todo if (MinecraftServer.getServer().isDedicatedServer())
        {
            serverVersion = "MinecraftForge (MC: " + MinecraftServer.getServer().getMinecraftVersion() + ")";
        }
        else
        {
            serverVersion = "MinecraftForgeSSP (MC: " + MinecraftServer.getServer().getMinecraftVersion() + ")";
        }*/
        int playersOnline = 1;//todo MinecraftServer.getServer().getCurrentPlayerCount();

        // END server software specific section -- all code below does not use any code outside of this class / Java

        // Construct the post data
        StringBuilder json = new StringBuilder(1024);
        json.append('{');

        // The plugin's description file containg all of the plugin data such as name, version, author, etc
        appendJSONPair(json, "guid", guid);
        appendJSONPair(json, "plugin_version", pluginVersion);
        appendJSONPair(json, "server_version", serverVersion);
        appendJSONPair(json, "players_online", Integer.toString(playersOnline));

        // New data as of R6
        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
        String osversion = System.getProperty("os.version");
        String java_version = System.getProperty("java.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        // normalize os arch .. amd64 -> x86_64
        if (osarch.equals("amd64"))
        {
            osarch = "x86_64";
        }

        appendJSONPair(json, "osname", osname);
        appendJSONPair(json, "osarch", osarch);
        appendJSONPair(json, "osversion", osversion);
        appendJSONPair(json, "cores", Integer.toString(coreCount));
        appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
        appendJSONPair(json, "java_version", java_version);

        // If we're pinging, append it
        if (isPing)
        {
            appendJSONPair(json, "ping", "1");
        }

        if (graphs.size() > 0)
        {
            synchronized (graphs)
            {
                json.append(',');
                json.append('"');
                json.append("graphs");
                json.append('"');
                json.append(':');
                json.append('{');

                boolean firstGraph = true;

                final Iterator<Graph> iter = graphs.iterator();

                while (iter.hasNext())
                {
                    Graph graph = iter.next();

                    StringBuilder graphJson = new StringBuilder();
                    graphJson.append('{');

                    for (Plotter plotter : graph.getPlotters())
                    {
                        appendJSONPair(graphJson, plotter.getColumnName(), Integer.toString(plotter.getValue()));
                    }

                    graphJson.append('}');

                    if (!firstGraph)
                    {
                        json.append(',');
                    }

                    json.append(escapeJSON(graph.getName()));
                    json.append(':');
                    json.append(graphJson);

                    firstGraph = false;
                }

                json.append('}');
            }
        }

        // close json
        json.append('}');

        // Create the url
        URL url = new URL(BASE_URL + String.format(REPORT_URL, urlEncode(pluginName)));

        // Connect to the website
        URLConnection connection;

        // Mineshafter creates a socks proxy, so we can safely bypass it
        // It does not reroute POST requests so we need to go around it
        if (isMineshafterPresent())
        {
            connection = url.openConnection(Proxy.NO_PROXY);
        }
        else
        {
            connection = url.openConnection();
        }


        byte[] uncompressed = json.toString().getBytes();
        byte[] compressed = gzip(json.toString());

        // Headers
        connection.addRequestProperty("User-Agent", "MCStats/" + REVISION);
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");

        connection.setDoOutput(true);

        if (debug)
        {
            System.out.println("[Metrics] Prepared request for " + pluginName + " uncompressed=" + uncompressed.length + " compressed=" + compressed.length);
        }

        // Write the data
        OutputStream os = connection.getOutputStream();
        os.write(compressed);
        os.flush();

        // Now read the response
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = reader.readLine();

        // close resources
        os.close();
        reader.close();

        if (response == null || response.startsWith("ERR") || response.startsWith("7"))
        {
            if (response == null)
            {
                response = "null";
            }
            else if (response.startsWith("7"))
            {
                response = response.substring(response.startsWith("7,") ? 2 : 1);
            }

            throw new IOException(response);
        }
        else
        {
            // Is this the first update this hour?
            if (response.equals("1") || response.contains("This is your first update this hour"))
            {
                synchronized (graphs)
                {
                    final Iterator<Graph> iter = graphs.iterator();

                    while (iter.hasNext())
                    {
                        final Graph graph = iter.next();

                        for (Plotter plotter : graph.getPlotters())
                        {
                            plotter.reset();
                        }
                    }
                }
            }
        }
    }

    /**
     * GZip compress a string of bytes
     *
     * @param input
     * @return
     */
    public static byte[] gzip(String input)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = null;

        try
        {
            gzos = new GZIPOutputStream(baos);
            gzos.write(input.getBytes("UTF-8"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (gzos != null) try
            {
                gzos.close();
            }
            catch (IOException ignore)
            {
            }
        }

        return baos.toByteArray();
    }

    /**
     * Check if mineshafter is present. If it is, we need to bypass it to send POST requests
     *
     * @return true if mineshafter is installed on the server
     */
    private boolean isMineshafterPresent()
    {
        try
        {
            Class.forName("mineshafter.MineServer");
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Appends a json encoded key/value pair to the given string builder.
     *
     * @param json
     * @param key
     * @param value
     * @throws java.io.UnsupportedEncodingException
     */
    private static void appendJSONPair(StringBuilder json, String key, String value) throws UnsupportedEncodingException
    {
        boolean isValueNumeric = false;

        try
        {
            if (value.equals("0") || !value.endsWith("0"))
            {
                Double.parseDouble(value);
                isValueNumeric = true;
            }
        }
        catch (NumberFormatException e)
        {
            isValueNumeric = false;
        }

        if (json.charAt(json.length() - 1) != '{')
        {
            json.append(',');
        }

        json.append(escapeJSON(key));
        json.append(':');

        if (isValueNumeric)
        {
            json.append(value);
        }
        else
        {
            json.append(escapeJSON(value));
        }
    }

    /**
     * Escape a string to create a valid JSON string
     *
     * @param text
     * @return
     */
    private static String escapeJSON(String text)
    {
        StringBuilder builder = new StringBuilder();

        builder.append('"');
        for (int index = 0; index < text.length(); index++)
        {
            char chr = text.charAt(index);

            switch (chr)
            {
                case '"':
                case '\\':
                    builder.append('\\');
                    builder.append(chr);
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    if (chr < ' ')
                    {
                        String t = "000" + Integer.toHexString(chr);
                        builder.append("\\u" + t.substring(t.length() - 4));
                    }
                    else
                    {
                        builder.append(chr);
                    }
                    break;
            }
        }
        builder.append('"');

        return builder.toString();
    }

    /**
     * Encode text as UTF-8
     *
     * @param text the text to encode
     * @return the encoded text, as UTF-8
     */
    private static String urlEncode(final String text) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(text, "UTF-8");
    }

    /**
     * Represents a custom graph on the website
     */
    public static class Graph
    {

        /**
         * The graph's name, alphanumeric and spaces only :) If it does not comply to the above when submitted, it is
         * rejected
         */
        private final String name;

        /**
         * The set of plotters that are contained within this graph
         */
        private final Set<Plotter> plotters = new LinkedHashSet<Plotter>();

        private Graph(final String name)
        {
            this.name = name;
        }

        /**
         * Gets the graph's name
         *
         * @return the Graph's name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Add a plotter to the graph, which will be used to plot entries
         *
         * @param plotter the plotter to add to the graph
         */
        public void addPlotter(final Plotter plotter)
        {
            plotters.add(plotter);
        }

        /**
         * Remove a plotter from the graph
         *
         * @param plotter the plotter to remove from the graph
         */
        public void removePlotter(final Plotter plotter)
        {
            plotters.remove(plotter);
        }

        /**
         * Gets an <b>unmodifiable</b> set of the plotter objects in the graph
         *
         * @return an unmodifiable {@link java.util.Set} of the plotter objects
         */
        public Set<Plotter> getPlotters()
        {
            return Collections.unmodifiableSet(plotters);
        }

        @Override
        public int hashCode()
        {
            return name.hashCode();
        }

        @Override
        public boolean equals(final Object object)
        {
            if (!(object instanceof Graph))
            {
                return false;
            }

            final Graph graph = (Graph) object;
            return graph.name.equals(name);
        }

        /**
         * Called when the server owner decides to opt-out of BukkitMetrics while the server is running.
         */
        protected void onOptOut()
        {
        }
    }

    /**
     * Interface used to collect custom data for a plugin
     */
    public static abstract class Plotter
    {

        /**
         * The plot's name
         */
        private final String name;

        /**
         * Construct a plotter with the default plot name
         */
        public Plotter()
        {
            this("Default");
        }

        /**
         * Construct a plotter with a specific plot name
         *
         * @param name the name of the plotter to use, which will show up on the website
         */
        public Plotter(final String name)
        {
            this.name = name;
        }

        /**
         * Get the current value for the plotted point. Since this function defers to an external function it may or may
         * not return immediately thus cannot be guaranteed to be thread friendly or safe. This function can be called
         * from any thread so care should be taken when accessing resources that need to be synchronized.
         *
         * @return the current value for the point to be plotted.
         */
        public abstract int getValue();

        /**
         * Get the column name for the plotted point
         *
         * @return the plotted point's column name
         */
        public String getColumnName()
        {
            return name;
        }

        /**
         * Called after the website graphs have been updated
         */
        public void reset()
        {
        }

        @Override
        public int hashCode()
        {
            return getColumnName().hashCode();
        }

        @Override
        public boolean equals(final Object object)
        {
            if (!(object instanceof Plotter))
            {
                return false;
            }

            final Plotter plotter = (Plotter) object;
            return plotter.name.equals(name) && plotter.getValue() == getValue();
        }
    }
}
