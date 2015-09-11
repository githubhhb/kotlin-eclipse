package org.jetbrains.kotlin.ui.editors

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.views.contentoutline.IContentOutlinePage
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.ui.debug.KotlinToggleBreakpointAdapter
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectNextAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectPreviousAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSemanticSelectionAction
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import kotlin.properties.Delegates
import kotlin.lazy
import java.lang.Class
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.openapi.util.text.StringUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.JetFile

public class KotlinClassFileEditor:ClassFileEditor(), KotlinEditor {
    override fun isEditable() = false

    override val javaEditor = this

    override val parsedFile: JetFile
        get() {
            val environment = KotlinEnvironment.getEnvironment(javaProject);
            val ideaProject = environment.getProject();
            return JetPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(),"\n"))
        }

    override val javaProject: IJavaProject
        get() = classFile.getJavaProject()

    private val colorManager:IColorManager
    
    private val kotlinOutlinePage:KotlinOutlinePage by lazy {
        KotlinOutlinePage(this)
    }
    
    private val kotlinToggleBreakpointAdapter:KotlinToggleBreakpointAdapter by lazy {
        KotlinToggleBreakpointAdapter()
    }
    
    init {
        colorManager = JavaColorManager()
    }

    override public fun getAdapter(required: Class<*>):Any? =
        when (required) {
            IContentOutlinePage::class.java -> kotlinOutlinePage
            IToggleBreakpointsTarget::class.java -> kotlinToggleBreakpointAdapter
            else -> super<ClassFileEditor>.getAdapter(required)
        }

    override public fun createPartControl(parent:Composite) {
        setSourceViewerConfiguration(Configuration(colorManager, this, getPreferenceStore()))
        super<ClassFileEditor>.createPartControl(parent)
    }

    override protected fun isMarkingOccurrences() = false

    override protected fun isTabsToSpacesConversionEnabled() =
        IndenterUtil.isSpacesForTabs()

    override protected fun createActions() {
        super<ClassFileEditor>.createActions()

        val selectionHistory = SelectionHistory(this)
        val historyAction = StructureSelectHistoryAction(this, selectionHistory)
        historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST)
        setAction(KotlinSemanticSelectionAction.HISTORY, historyAction)
        selectionHistory.setHistoryAction(historyAction)

        setAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT, KotlinOpenDeclarationAction(this))

        setAction(KotlinSelectEnclosingAction.SELECT_ENCLOSING_TEXT, KotlinSelectEnclosingAction(this, selectionHistory))
        setAction(KotlinSelectPreviousAction.SELECT_PREVIOUS_TEXT, KotlinSelectPreviousAction(this, selectionHistory))
        setAction(KotlinSelectNextAction.SELECT_NEXT_TEXT, KotlinSelectNextAction(this, selectionHistory))
    }

    override public fun dispose() {
        colorManager.dispose()
        super<ClassFileEditor>.dispose()
    }

    override public fun setSelection(element:IJavaElement) {
        KotlinOpenEditor.revealKotlinElement(this, element)
    }

    companion object {
        public val EDITOR_ID:String = "org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor"
    }

    private val classFile: IClassFile
        get() = getEditorInput().getAdapter(IJavaElement::class.java) as IClassFile

    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
}