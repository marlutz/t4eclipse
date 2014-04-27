package ch.mlutz.plugins.t4e.tapestry.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import ch.mlutz.plugins.t4e.log.EclipseLogFactory;
import ch.mlutz.plugins.t4e.log.IEclipseLog;
import ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.DelegatingHyperlinkDetector;
import ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.IHyperlinkDetectorDelegate;
import ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.JwcidHyperlinkDetectorDelegate;
import ch.mlutz.plugins.t4e.tapestry.editor.hyperlink.OgnlHyperlinkDetectorDelegate;

public class TapestryConfiguration extends TextSourceViewerConfiguration {

	private static final IEclipseLog log=
			EclipseLogFactory.create(TapestryConfiguration.class);

	private TapestryDoubleClickStrategy doubleClickStrategy;
	private TapestryTagScanner tagScanner;
	private TapestryScanner scanner;
	private ColorManager colorManager;

	public TapestryConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}

	/*
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer)
	{
		return new String[]
		{
				IDocument.DEFAULT_CONTENT_TYPE,
				XMLPartitionScanner.XML_COMMENT,
				XMLPartitionScanner.XML_PI,
				XMLPartitionScanner.XML_DOCTYPE,
				XMLPartitionScanner.XML_START_TAG,
				XMLPartitionScanner.XML_END_TAG,
				XMLPartitionScanner.XML_TEXT
		};
	}

	/*/
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			TapestryPartitionScanner.XML_COMMENT,
			TapestryPartitionScanner.XML_TAG };
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new TapestryDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected TapestryScanner getXMLScanner() {
		if (scanner == null) {
			scanner = new TapestryScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IXMLColorConstants.DEFAULT))));
		}
		return scanner;
	}
	protected TapestryTagScanner getXMLTagScanner() {
		if (tagScanner == null) {
			tagScanner = new TapestryTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IXMLColorConstants.TAG))));
		}
		return tagScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, TapestryPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, TapestryPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
		reconciler.setDamager(ndr, TapestryPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, TapestryPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer)
	{

		ContentAssistant assistant = new ContentAssistant();

		IContentAssistProcessor tagContentAssistProcessor
			= new TagContentAssistProcessor(getXMLTagScanner());
		assistant.setContentAssistProcessor(tagContentAssistProcessor,
				TapestryPartitionScanner.XML_TAG);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);
		assistant.setProposalPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);
		return assistant;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		IHyperlinkDetector[] result= super.getHyperlinkDetectors(sourceViewer);
		List<IHyperlinkDetector> resultList= new ArrayList<IHyperlinkDetector>();
		resultList.addAll(Arrays.asList(result));

		DelegatingHyperlinkDetector detector=
				new DelegatingHyperlinkDetector(
						new HashSet<String>(Arrays.asList(
								TapestryPartitionScanner.XML_TAG)));
		detector.addDelegate(new JwcidHyperlinkDetectorDelegate());
		detector.addDelegate(new OgnlHyperlinkDetectorDelegate());

		resultList.add(detector);
		return resultList.toArray(new IHyperlinkDetector[resultList.size()]);
	}

	/*
	@Override
	protected Map<String, IAdaptable> getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
		log.info("getHyperlinkDetectorTargets called with parameter " + sourceViewer.getClass());

		@SuppressWarnings("unchecked")
		Map<String, IAdaptable> result= super.getHyperlinkDetectorTargets(sourceViewer);
		result.put("ch.mlutz.plugins.t4e.editors.tapestryTarget",
				(IAdaptable) sourceViewer);
		return result;
	}
	*/
}