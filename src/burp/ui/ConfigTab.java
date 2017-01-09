package burp.ui;

import burp.ITab;
import burp.eventStream.Config;
import burp.eventStream.EventStream;
import burp.eventStream.FormUtility;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConfigTab implements ITab
{
    private final Config config;
    private final ReconfigurableEventStream eventStream;
    private final JPanel panel;
    private final JPanel frame;

    private FormUtility formUtility = new FormUtility();

    public ConfigTab(Config config, ReconfigurableEventStream eventStream)
    {
        this.config = config;
        this.eventStream = eventStream;

        panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        frame = new JPanel(new BorderLayout());
        frame.add(panel, BorderLayout.NORTH);
    }

    @Override
    public String getTabCaption()
    {
        return "ElasticSearch";
    }

    @Override
    public Component getUiComponent()
    {
        addRowType(Config.TYPE_KEY, config.type(), new ComboBoxFieldActionListener()
        {
            @Override
            public void action(JComboBox<EventStream.Type> comboBox)
            {
                config.type((EventStream.Type) comboBox.getSelectedItem());
                eventStream.eventStreamTypeChanged();
            }
        });

        addRowText(Config.ELASTICSEARCH_HOST_KEY, config.elasticSearchHost(), new PlainDocument(), new TextFieldActionListener()
        {
            @Override
            public void action(JTextField textField)
            {
                config.elasticSearchHost(textField.getText());
            }
        });

        addRowText(Config.ELASTICSEARCH_PORT_KEY, new Integer(config.elasticSearchPort()).toString(), new PlainDocument(), new TextFieldActionListener()
        {
            @Override
            public void action(JTextField textField)
            {
                config.elasticSearchPort(Integer.parseInt(textField.getText()));
            }
        });

        addRowText(Config.ELASTICSEARCH_PREFIX_KEY, config.elasticSearchPrefix(), new PlainDocument(), new TextFieldActionListener()
        {
            @Override
            public void action(JTextField textField)
            {
                config.elasticSearchPrefix(textField.getText());
            }
        });

        return frame;
    }

    private void addRowText(String key, String val, Document document, final TextFieldActionListener textFieldActionListener)
    {
        final JTextField textField = new JTextField(document, val, 1);

        textField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                textFieldActionListener.action(textField);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                textFieldActionListener.action(textField);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                textFieldActionListener.action(textField);
            }
        });

        formUtility.addLabel(new JLabel(outputKey(key)), panel);
        formUtility.addLastField(textField, panel);
    }

    private void addRowType(String key, EventStream.Type val, final ComboBoxFieldActionListener comboBoxFieldActionListener)
    {
        final JComboBox<EventStream.Type> comboBox = new JComboBox<EventStream.Type>(EventStream.Type.values());
        comboBox.setSelectedItem(val);

        comboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                comboBoxFieldActionListener.action(comboBox);
            }
        });

        formUtility.addLabel(new JLabel(outputKey(key)), panel);
        formUtility.addLastField(comboBox, panel);

    }

    private interface TextFieldActionListener
    {
        void action(JTextField textField);
    }

    private interface ComboBoxFieldActionListener
    {
        void action(JComboBox<EventStream.Type> comboBox);
    }

    private static String outputKey(String key)
    {
        char[] array = key.toLowerCase().replace("_", " ").toCharArray();
        array[0] = Character.toUpperCase(array[0]);

        for (int i = 1; i < array.length; i++) {
            if (Character.isWhitespace(array[i - 1])) {
                array[i] = Character.toUpperCase(array[i]);
            }
        }

        return new String(array);
    }
}
