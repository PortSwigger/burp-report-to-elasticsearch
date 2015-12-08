package burp;

import burp.ui.ConfigTab;
import burp.ui.ReconfigurableEventStream;
import burp.ui.ScanState;
import burp.eventStream.Config;
import burp.scanWatcher.ScanWatcher;

import java.util.Map;
import java.util.concurrent.Executors;

import static burp.IBurpExtenderCallbacks.TOOL_SCANNER;

public class BurpExtender implements IBurpExtender, IScannerListener, IHttpListener
{
    private Config config;
    private ScanWatcher scanWatcher;
    private ScanState scanState;
    private ReconfigurableEventStream eventStream;

    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {
        callbacks.printOutput("TRACE: cooldown time after last scanner behaviour " + guessCooldownTime(new DefaultedConfig(callbacks.saveConfig(), new DefaultedConfig.Output()
        {
            @Override
            public void println(String str)
            {
                callbacks.printOutput(str);
            }
        })) + "ms");

        config = Config.ConfigFactory.Build(new Config.Output()
        {
            @Override
            public void println(String str)
            {
                callbacks.printOutput(str);
            }
        });

        eventStream = new ReconfigurableEventStream(config, callbacks);

        scanState = new ScanState();

        scanWatcher = new ScanWatcher(
                new ScanWatcher.Platform()
                {
                    @Override
                    public long currentTimeMs()
                    {
                        return System.currentTimeMillis();
                    }

                    @Override
                    public void startOnNewThread(String name, Runnable runnable)
                    {
                        final Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                        thread.setName(name);
                        thread.start();
                    }

                    @Override
                    public void sleep(int ms)
                    {
                        try
                        {
                            Thread.sleep(ms);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                },
                new ScanWatcher.CooldownCalculator()
                {
                    @Override
                    public long milliseconds()
                    {
                        final DefaultedConfig burpConfig = new DefaultedConfig(callbacks.saveConfig(), new DefaultedConfig.Output()
                        {
                            @Override
                            public void println(String str)
                            {
                                callbacks.printOutput(str);
                            }
                        });

                        return guessCooldownTime(burpConfig);
                    }
                },
                new ScanWatcher.Events()
                {
                    @Override
                    public void onScanStart()
                    {
                        scanState.onScanStart(System.currentTimeMillis());
                        eventStream.onScanStart(scanState.currentScanId(), scanState.startTimeMs());
                    }

                    @Override
                    public void onScanEnd()
                    {
                        final long startTimeMs = scanState.startTimeMs();
                        final long endTimeMs = System.currentTimeMillis();

                        eventStream.onScanEnd(scanState.currentScanId(), startTimeMs, endTimeMs, (endTimeMs - startTimeMs) / 1000);
                    }
                }, 5000
        );


        callbacks.registerScannerListener(this);
        callbacks.registerHttpListener(this);

        callbacks.addSuiteTab(new ConfigTab(config, eventStream));
    }

    private static long guessCooldownTime(DefaultedConfig burpConfig)
    {
        return (burpConfig.getLong("scanner.maxanalysistimeperitem", 0) * 1000) +
                burpConfig.getLong("scanner.throttleinterval", 0) +
                burpConfig.getLong("scanner.pausebeforeretry", 0) +
                burpConfig.getLong("suite.normaltimeoutmilli", 0)
        ;
    }

    private static class DefaultedConfig
    {
        final Map<String, String> burpConfig;
        private final Output output;

        DefaultedConfig(Map<String, String> burpConfig, Output output)
        {
            this.burpConfig = burpConfig;
            this.output = output;
        }

        public long getLong(String key, long defaultValue)
        {
            String val = burpConfig.get(key);

            if (val == null)
            {
                output.println("TRACE: missing config value for " + key);
                return defaultValue;
            }

            try
            {
                return Long.parseLong(val);
            }
            catch (NumberFormatException ex)
            {
                output.println("TRACE: invalid config value for " + key + "; " + val);
                return defaultValue;
            }
        }

        public interface Output
        {
            void println(String str);
        }
    }


    private static String generateScanId()
    {
        return "abc123";
    }


    @Override
    public void newScanIssue(IScanIssue issue)
    {
        scanWatcher.onScannerActivity();
        eventStream.onIssueDiscovered(scanState.currentScanId(), issue);
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo)
    {
        if ((toolFlag & TOOL_SCANNER) != TOOL_SCANNER)
        {
            return; // not interested in non-scanner messages
        }

        if (!messageIsRequest)
        {
            return; // not interested in responses
        }

        scanWatcher.onScannerActivity();
    }
}