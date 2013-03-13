package parser;

import CoreNLP.*;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreePrint;

public class StanfordParserThrift
{

    private LexicalizedParser parser;
    private boolean customOutputOptionsSet;
//    private boolean customParserOptionsSet;
    private TreePrint treePrinter;

    public StanfordParserThrift(String modelFile) 
    {
        loadModel(modelFile);
        treePrinter = new TreePrint("oneline", "", new PennTreebankLanguagePack());
        customOutputOptionsSet = false;
//        customParserOptionsSet = false;
    }

    private void loadModel(String modelFile)
    {
        if (modelFile.equals("") || modelFile == null) {
            parser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", new String[]{});
        }
        else {
            parser = LexicalizedParser.loadModel(modelFile, new String[]{});
        }
    }

    private void setOptions(List<String> outputOptions) throws Exception
    {
        String outputFormatStr = "oneline";
        String outputFormatOptionsStr = "";

        // for output formatting
        if (outputOptions.size() > 0 || outputOptions != null)
        {
        	int ofIndex = outputOptions.indexOf("-outputFormat");
        	int ofoIndex = outputOptions.indexOf("-outputFormatOptions");
        	
        	if (ofIndex >= 0)
        	{
        		outputFormatStr = outputOptions.get(ofIndex+1);
        	}
        	if (ofoIndex >= 0)
        	{
        		outputFormatOptionsStr = outputOptions.get(ofoIndex+1);
        	}
        	if (ofIndex < 0 && ofoIndex < 0)
        	{
        		throw new Exception("Invalid option(s): " + outputOptions.toString());
        	}
        	
            customOutputOptionsSet = true;
        }
        else
        {
        	customOutputOptionsSet = false;
        }
        
        treePrinter = new TreePrint(outputFormatStr, outputFormatOptionsStr, new PennTreebankLanguagePack());

        // for everything else; disabled for now
//        if (!options.isEmpty())
//        {
//        	String[] remainingOptions = new String[options.size()];
//        	options.toArray(remainingOptions);
//        	parser.setOptionFlags(remainingOptions);
//        	customParserOptionsSet = true;
//        }
    }

//    private void resetOptions()
//    {
//        if (customParserOptionsSet)
//        {
//            loadModel(modelFile);
//            customParserOptionsSet = false;
//        }
//
//        if (customOutputOptionsSet)
//        {
//            treePrinter = new TreePrint("oneline", "", new PennTreebankLanguagePack());
//            customOutputOptionsSet = false;
//        }
//    }

    public List<ParseTree> parse_text(String text, List<String> outputFormat) throws TApplicationException
    {
        List<ParseTree> results = new ArrayList<ParseTree>();
        
        try
        {
            if (outputFormat != null && outputFormat.size() > 0)
            {
            	setOptions(outputFormat);
            }
            else
            {
            	if (customOutputOptionsSet)
            	{
            		setOptions(null);
            	}
            }
            
        	// assume no tokenization was done; use Stanford's default tokenizer
        	DocumentPreprocessor preprocess = new DocumentPreprocessor(new StringReader(text));
        	Iterator<List<HasWord>> foundSentences = preprocess.iterator();
        	while (foundSentences.hasNext())
        	{
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
        		Tree parseTree = parser.apply(foundSentences.next());
        		treePrinter.printTree(parseTree, pw);
        		results.add(new ParseTree(sw.getBuffer().toString().trim(), parseTree.score()));
        	}
        }
        catch (Exception e)
        {
        	// FIXME
        	throw new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
        }

        return results;
    }

    public ParseTree parse_tokens(List<String> tokens, List<String> outputFormat) throws TApplicationException
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
//        List<ParseTree> results = new ArrayList<ParseTree>();
    	
    	// assume an array of tokens was passed in
        // This doesn't seem to be getting used much; the typical case is to pass in one sentence worth of tokens.
        // This code here handled the case where you wanted two parse trees, one for each sentence "This is a sentence.  It is about cats."
        // and you wanted to pass in [["This", "is", "a", "sentence", "."], ["It", "is", "about", "cats", "."]]
        // but instead this code is looking for ["This", "is", "a", "sentence", ".", "\n", "It", "is", "about", "cats", "."]
    	/*if (tokens.contains("\n"))
    	{
    		StringBuilder builder = new StringBuilder();
    		// at least one sentence worth of tokens
    		for(String token : tokens)
    		{
    			builder.append(token+" ");
    		}
    		String[] multipleSentences = builder.toString().split("\n");
    		for (String s : multipleSentences)
    		{
    			try
    			{
    				List<CoreLabel> crazyStanfordFormat = Sentence.toCoreLabelList(s.trim().split(" "));
    				Tree parseTree = parser.apply(crazyStanfordFormat);
    				treePrinter.printTree(parseTree, pw);
    				results.add(new ParseTree(sw.getBuffer().toString().trim(), parseTree.score()));
    			}
    			catch (Exception e)
    			{
    				throw new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
    			}
    		}
    	}
    	else
    	{*/
        try
        {
            if (outputFormat != null && outputFormat.size() > 0)
            {
            	setOptions(outputFormat);
            }
            else
            {
            	if (customOutputOptionsSet)
            	{
            		setOptions(null);
            	}
            }
        	
        	// a single sentence worth of tokens
        	String[] tokenArray = new String[tokens.size()];
        	tokens.toArray(tokenArray);
        	List<CoreLabel> crazyStanfordFormat = Sentence.toCoreLabelList(tokenArray);
        	Tree parseTree = parser.apply(crazyStanfordFormat);
        	treePrinter.printTree(parseTree, pw);
//        	results.add(new ParseTree(sw.getBuffer().toString().trim(), parseTree.score()));
        	return new ParseTree(sw.getBuffer().toString().trim(), parseTree.score());
        }
        catch (Exception e)
        {
        	// FIXME
        	throw new TApplicationException(TApplicationException.INTERNAL_ERROR, e.getMessage());
//        	try
//        	{
//        		ByteArrayOutputStream os = new ByteArrayOutputStream();
//        		new ObjectOutputStream(os).writeObject(e);
//        		throw new SerializedException(ByteBuffer.wrap(os.toByteArray()));
//        	}
//        	catch (Exception ex)
//        	{
//        		System.err.println(ex.getMessage());
//        	}
        }
    	//}
//        return null;
//    	return results;
    }
    
//    public void ping() {
//        System.out.println("ping()");
//    }
//
//    public void zip() {
//        System.out.println("zip()");
//    }
}

