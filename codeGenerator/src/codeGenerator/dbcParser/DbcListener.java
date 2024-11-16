// Generated from c:\home\vranken\projects\comFrameworkAtSourceforge\codeGenerator\trunk\src/codeGenerator/dbcParser/Dbc.g4 by ANTLR 4.12.0
package codeGenerator.dbcParser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DbcParser}.
 */
public interface DbcListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DbcParser#dbc}.
	 * @param ctx the parse tree
	 */
	void enterDbc(DbcParser.DbcContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#dbc}.
	 * @param ctx the parse tree
	 */
	void exitDbc(DbcParser.DbcContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#version}.
	 * @param ctx the parse tree
	 */
	void enterVersion(DbcParser.VersionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#version}.
	 * @param ctx the parse tree
	 */
	void exitVersion(DbcParser.VersionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#keyword}.
	 * @param ctx the parse tree
	 */
	void enterKeyword(DbcParser.KeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#keyword}.
	 * @param ctx the parse tree
	 */
	void exitKeyword(DbcParser.KeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#newSymbol}.
	 * @param ctx the parse tree
	 */
	void enterNewSymbol(DbcParser.NewSymbolContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#newSymbol}.
	 * @param ctx the parse tree
	 */
	void exitNewSymbol(DbcParser.NewSymbolContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#newSymbols}.
	 * @param ctx the parse tree
	 */
	void enterNewSymbols(DbcParser.NewSymbolsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#newSymbols}.
	 * @param ctx the parse tree
	 */
	void exitNewSymbols(DbcParser.NewSymbolsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#nodes}.
	 * @param ctx the parse tree
	 */
	void enterNodes(DbcParser.NodesContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#nodes}.
	 * @param ctx the parse tree
	 */
	void exitNodes(DbcParser.NodesContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#valueTable}.
	 * @param ctx the parse tree
	 */
	void enterValueTable(DbcParser.ValueTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#valueTable}.
	 * @param ctx the parse tree
	 */
	void exitValueTable(DbcParser.ValueTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#msg}.
	 * @param ctx the parse tree
	 */
	void enterMsg(DbcParser.MsgContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#msg}.
	 * @param ctx the parse tree
	 */
	void exitMsg(DbcParser.MsgContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#pseudoMsg}.
	 * @param ctx the parse tree
	 */
	void enterPseudoMsg(DbcParser.PseudoMsgContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#pseudoMsg}.
	 * @param ctx the parse tree
	 */
	void exitPseudoMsg(DbcParser.PseudoMsgContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signal}.
	 * @param ctx the parse tree
	 */
	void enterSignal(DbcParser.SignalContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signal}.
	 * @param ctx the parse tree
	 */
	void exitSignal(DbcParser.SignalContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#dummyNode}.
	 * @param ctx the parse tree
	 */
	void enterDummyNode(DbcParser.DummyNodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#dummyNode}.
	 * @param ctx the parse tree
	 */
	void exitDummyNode(DbcParser.DummyNodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signalExtendedValueTypeList}.
	 * @param ctx the parse tree
	 */
	void enterSignalExtendedValueTypeList(DbcParser.SignalExtendedValueTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signalExtendedValueTypeList}.
	 * @param ctx the parse tree
	 */
	void exitSignalExtendedValueTypeList(DbcParser.SignalExtendedValueTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#messageTransmitter}.
	 * @param ctx the parse tree
	 */
	void enterMessageTransmitter(DbcParser.MessageTransmitterContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#messageTransmitter}.
	 * @param ctx the parse tree
	 */
	void exitMessageTransmitter(DbcParser.MessageTransmitterContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#valueDescription}.
	 * @param ctx the parse tree
	 */
	void enterValueDescription(DbcParser.ValueDescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#valueDescription}.
	 * @param ctx the parse tree
	 */
	void exitValueDescription(DbcParser.ValueDescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#singleValueDescription}.
	 * @param ctx the parse tree
	 */
	void enterSingleValueDescription(DbcParser.SingleValueDescriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#singleValueDescription}.
	 * @param ctx the parse tree
	 */
	void exitSingleValueDescription(DbcParser.SingleValueDescriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#environmentVariable}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentVariable(DbcParser.EnvironmentVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#environmentVariable}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentVariable(DbcParser.EnvironmentVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#environmentVariableData}.
	 * @param ctx the parse tree
	 */
	void enterEnvironmentVariableData(DbcParser.EnvironmentVariableDataContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#environmentVariableData}.
	 * @param ctx the parse tree
	 */
	void exitEnvironmentVariableData(DbcParser.EnvironmentVariableDataContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#categoryDefinition}.
	 * @param ctx the parse tree
	 */
	void enterCategoryDefinition(DbcParser.CategoryDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#categoryDefinition}.
	 * @param ctx the parse tree
	 */
	void exitCategoryDefinition(DbcParser.CategoryDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#category}.
	 * @param ctx the parse tree
	 */
	void enterCategory(DbcParser.CategoryContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#category}.
	 * @param ctx the parse tree
	 */
	void exitCategory(DbcParser.CategoryContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(DbcParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(DbcParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signalType}.
	 * @param ctx the parse tree
	 */
	void enterSignalType(DbcParser.SignalTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signalType}.
	 * @param ctx the parse tree
	 */
	void exitSignalType(DbcParser.SignalTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signalTypeRef}.
	 * @param ctx the parse tree
	 */
	void enterSignalTypeRef(DbcParser.SignalTypeRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signalTypeRef}.
	 * @param ctx the parse tree
	 */
	void exitSignalTypeRef(DbcParser.SignalTypeRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signalGroup}.
	 * @param ctx the parse tree
	 */
	void enterSignalGroup(DbcParser.SignalGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signalGroup}.
	 * @param ctx the parse tree
	 */
	void exitSignalGroup(DbcParser.SignalGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(DbcParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(DbcParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#globalComment}.
	 * @param ctx the parse tree
	 */
	void enterGlobalComment(DbcParser.GlobalCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#globalComment}.
	 * @param ctx the parse tree
	 */
	void exitGlobalComment(DbcParser.GlobalCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#nodeComment}.
	 * @param ctx the parse tree
	 */
	void enterNodeComment(DbcParser.NodeCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#nodeComment}.
	 * @param ctx the parse tree
	 */
	void exitNodeComment(DbcParser.NodeCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#msgComment}.
	 * @param ctx the parse tree
	 */
	void enterMsgComment(DbcParser.MsgCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#msgComment}.
	 * @param ctx the parse tree
	 */
	void exitMsgComment(DbcParser.MsgCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signalComment}.
	 * @param ctx the parse tree
	 */
	void enterSignalComment(DbcParser.SignalCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signalComment}.
	 * @param ctx the parse tree
	 */
	void exitSignalComment(DbcParser.SignalCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#envVarComment}.
	 * @param ctx the parse tree
	 */
	void enterEnvVarComment(DbcParser.EnvVarCommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#envVarComment}.
	 * @param ctx the parse tree
	 */
	void exitEnvVarComment(DbcParser.EnvVarCommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attributeDefinition}.
	 * @param ctx the parse tree
	 */
	void enterAttributeDefinition(DbcParser.AttributeDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attributeDefinition}.
	 * @param ctx the parse tree
	 */
	void exitAttributeDefinition(DbcParser.AttributeDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attribTypeInt}.
	 * @param ctx the parse tree
	 */
	void enterAttribTypeInt(DbcParser.AttribTypeIntContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attribTypeInt}.
	 * @param ctx the parse tree
	 */
	void exitAttribTypeInt(DbcParser.AttribTypeIntContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attribTypeFloat}.
	 * @param ctx the parse tree
	 */
	void enterAttribTypeFloat(DbcParser.AttribTypeFloatContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attribTypeFloat}.
	 * @param ctx the parse tree
	 */
	void exitAttribTypeFloat(DbcParser.AttribTypeFloatContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attribTypeString}.
	 * @param ctx the parse tree
	 */
	void enterAttribTypeString(DbcParser.AttribTypeStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attribTypeString}.
	 * @param ctx the parse tree
	 */
	void exitAttribTypeString(DbcParser.AttribTypeStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attribTypeEnum}.
	 * @param ctx the parse tree
	 */
	void enterAttribTypeEnum(DbcParser.AttribTypeEnumContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attribTypeEnum}.
	 * @param ctx the parse tree
	 */
	void exitAttribTypeEnum(DbcParser.AttribTypeEnumContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attributeDefault}.
	 * @param ctx the parse tree
	 */
	void enterAttributeDefault(DbcParser.AttributeDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attributeDefault}.
	 * @param ctx the parse tree
	 */
	void exitAttributeDefault(DbcParser.AttributeDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void enterAttributeValue(DbcParser.AttributeValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void exitAttributeValue(DbcParser.AttributeValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#attribVal}.
	 * @param ctx the parse tree
	 */
	void enterAttribVal(DbcParser.AttribValContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#attribVal}.
	 * @param ctx the parse tree
	 */
	void exitAttribVal(DbcParser.AttribValContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#unrecognizedStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnrecognizedStatement(DbcParser.UnrecognizedStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#unrecognizedStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnrecognizedStatement(DbcParser.UnrecognizedStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#unrecognizedAttributeDefinitionNodeSpecific}.
	 * @param ctx the parse tree
	 */
	void enterUnrecognizedAttributeDefinitionNodeSpecific(DbcParser.UnrecognizedAttributeDefinitionNodeSpecificContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#unrecognizedAttributeDefinitionNodeSpecific}.
	 * @param ctx the parse tree
	 */
	void exitUnrecognizedAttributeDefinitionNodeSpecific(DbcParser.UnrecognizedAttributeDefinitionNodeSpecificContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#multiplexedSignal}.
	 * @param ctx the parse tree
	 */
	void enterMultiplexedSignal(DbcParser.MultiplexedSignalContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#multiplexedSignal}.
	 * @param ctx the parse tree
	 */
	void exitMultiplexedSignal(DbcParser.MultiplexedSignalContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(DbcParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(DbcParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link DbcParser#signedInteger}.
	 * @param ctx the parse tree
	 */
	void enterSignedInteger(DbcParser.SignedIntegerContext ctx);
	/**
	 * Exit a parse tree produced by {@link DbcParser#signedInteger}.
	 * @param ctx the parse tree
	 */
	void exitSignedInteger(DbcParser.SignedIntegerContext ctx);
}