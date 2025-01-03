// Generated from codeGenerator/dbcParser/Dbc.g4 by ANTLR 4.13.2
package codeGenerator.dbcParser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DbcParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DbcVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DbcParser#dbc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDbc(DbcParser.DbcContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVersion(DbcParser.VersionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyword(DbcParser.KeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#newSymbol}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewSymbol(DbcParser.NewSymbolContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#newSymbols}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewSymbols(DbcParser.NewSymbolsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#nodes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodes(DbcParser.NodesContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#valueTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueTable(DbcParser.ValueTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#msg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMsg(DbcParser.MsgContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#pseudoMsg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoMsg(DbcParser.PseudoMsgContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignal(DbcParser.SignalContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#dummyNode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDummyNode(DbcParser.DummyNodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signalExtendedValueTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalExtendedValueTypeList(DbcParser.SignalExtendedValueTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#messageTransmitter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessageTransmitter(DbcParser.MessageTransmitterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#valueDescription}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueDescription(DbcParser.ValueDescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#singleValueDescription}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleValueDescription(DbcParser.SingleValueDescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#environmentVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentVariable(DbcParser.EnvironmentVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#environmentVariableData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvironmentVariableData(DbcParser.EnvironmentVariableDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#categoryDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCategoryDefinition(DbcParser.CategoryDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#category}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCategory(DbcParser.CategoryContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(DbcParser.FilterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signalType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalType(DbcParser.SignalTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signalTypeRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalTypeRef(DbcParser.SignalTypeRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signalGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalGroup(DbcParser.SignalGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment(DbcParser.CommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#globalComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobalComment(DbcParser.GlobalCommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#nodeComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodeComment(DbcParser.NodeCommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#msgComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMsgComment(DbcParser.MsgCommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signalComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignalComment(DbcParser.SignalCommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#envVarComment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvVarComment(DbcParser.EnvVarCommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attributeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeDefinition(DbcParser.AttributeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attribTypeInt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribTypeInt(DbcParser.AttribTypeIntContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attribTypeFloat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribTypeFloat(DbcParser.AttribTypeFloatContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attribTypeString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribTypeString(DbcParser.AttribTypeStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attribTypeEnum}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribTypeEnum(DbcParser.AttribTypeEnumContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attributeDefault}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeDefault(DbcParser.AttributeDefaultContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attributeValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeValue(DbcParser.AttributeValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#attribVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribVal(DbcParser.AttribValContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#unrecognizedStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnrecognizedStatement(DbcParser.UnrecognizedStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#unrecognizedAttributeDefinitionNodeSpecific}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnrecognizedAttributeDefinitionNodeSpecific(DbcParser.UnrecognizedAttributeDefinitionNodeSpecificContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#multiplexedSignal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplexedSignal(DbcParser.MultiplexedSignalContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(DbcParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link DbcParser#signedInteger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignedInteger(DbcParser.SignedIntegerContext ctx);
}