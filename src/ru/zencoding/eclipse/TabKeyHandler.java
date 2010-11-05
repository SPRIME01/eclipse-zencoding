package ru.zencoding.eclipse;

import java.util.HashMap;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import ru.zencoding.eclipse.handlers.ExpandAbbreviationAction;

/**
 * Handles Tab key press
 * @author sergey
 *
 */
public class TabKeyHandler {
	private static HashMap<Integer, AbstractTextEditor> installedEditors = new HashMap<Integer, AbstractTextEditor>();
	private static HashMap<Integer, VerifyKeyListener> keyListeners = new HashMap<Integer, VerifyKeyListener>();
	private static boolean inited = false;
	private static boolean enabled = true;
	
	/**
	 * Tries to install key listener on editor's widget
	 */
	public static void install(IWorkbenchPart part) {
		IEditorPart editor;
		if (part instanceof IEditorPart) {
			editor = EclipseZenCodingHelper.getTextEditor((IEditorPart) part);
			if (editor instanceof AbstractTextEditor)
				install((AbstractTextEditor) editor);
		}
	}
	
	/**
	 * Tries to install key listener on editor's widget
	 */
	public static void install(AbstractTextEditor editor) {
		if (editor == null)
			return;
		
		Integer id = getEditorId(editor);
		if (!installedEditors.containsKey(id)) {
			// install key listener for Tab key
			try {
				ITextViewer textViewer = EclipseZenCodingHelper.getTextViewer(editor);
				StyledText widget = textViewer.getTextWidget();
				widget.addVerifyKeyListener(getKeyListener(editor));
				installedEditors.put(id, editor);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Uninstalls Tab key listener from editor
	 * @param editor
	 */
	public static void uninstall(AbstractTextEditor editor) {
		if (editor == null)
			return;
		
		Integer id = getEditorId(editor);
		if (installedEditors.containsKey(id)) {
			try {
				StyledText widget = EclipseZenCodingHelper.getTextViewer(editor).getTextWidget();
				widget.removeVerifyKeyListener(getKeyListener(editor));
				installedEditors.remove(id);
				keyListeners.remove(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void uninstall(IWorkbenchPart part) {
		IEditorPart editor;
		if (part instanceof IEditorPart) {
			editor = EclipseZenCodingHelper.getTextEditor((IEditorPart) part);
			if (editor instanceof AbstractTextEditor)
				uninstall((AbstractTextEditor) editor);
		}
	}
	
	/**
	 * Returns unique editor ID
	 * @param editor
	 * @return
	 */
	public static Integer getEditorId(AbstractTextEditor editor) {
		return editor.hashCode();
	}
	
	public static VerifyKeyListener getKeyListener(final AbstractTextEditor editor) {
		Integer id = getEditorId(editor);
		if (!keyListeners.containsKey(id)) {
			keyListeners.put(id, new VerifyKeyListener() {
				
				@Override
				public void verifyKey(VerifyEvent event) {
					IDocument document = EclipseZenCodingHelper.getDocument(editor);
					if (document == null) {
						return;
					}
					
					if (LinkedModeModel.hasInstalledModel(document)) {
						return;
					}
					
					if (event.doit && event.keyCode == 9 && ExpandAbbreviationAction.expand()) {
						event.doit = false; // cancel the event
					}
				}
			});
		}
		
		return keyListeners.get(id);
	}
	
	/**
	 * Setup global editor listener which adds Tab key listeners to newly 
	 * created editors
	 */
	public static void setup(IWorkbenchPage page) {
		if (!inited) {
			inited = true;
			page.addPartListener(new IPartListener() {
				
				@Override
				public void partOpened(IWorkbenchPart part) {
					if (isEnabled())
						install(part);
				}
				
				@Override
				public void partDeactivated(IWorkbenchPart part) {
					
				}
				
				@Override
				public void partClosed(IWorkbenchPart part) {
					uninstall(part);
				}
				
				@Override
				public void partBroughtToTop(IWorkbenchPart part) {
					
				}
				
				@Override
				public void partActivated(IWorkbenchPart part) {
					if (isEnabled())
						install(part);
				}
			});
		}
	}
	
	/**
	 * Try to install tab expander for all opened editors
	 */
	public static void installForAll() {
		IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (int i = 0; i < editors.length; i++) {
			IEditorReference editor = editors[i];
			install(editor.getEditor(false));
		}
	}
	
	/**
	 * Try to uninstall tab expander from all opened editors
	 */
	public static void uninstallFromAll() {
		IEditorReference[] editors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		for (int i = 0; i < editors.length; i++) {
			IEditorReference editor = editors[i];
			uninstall(editor.getEditor(false));
		}
	}

	public static void setEnabled(boolean enabled) {
		TabKeyHandler.enabled = enabled;
		if (enabled)
			installForAll();
		else
			uninstallFromAll();
	}

	public static boolean isEnabled() {
		return enabled;
	}
}
