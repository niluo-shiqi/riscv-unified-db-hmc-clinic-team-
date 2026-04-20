import * as assert from 'assert';
import * as vscode from 'vscode';
import * as path from 'path';

// Build an absolute path inside the test workspace (opened via runTests.ts)
function wsPath(...p: string[]) {
  const root = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath!;
  return path.join(root, ...p);
}

async function waitFor<T>(probe: () => T | null | undefined | false, ms = 8000, step = 50) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    const v = probe();
    if (v) return v;
    await new Promise(r => setTimeout(r, step));
  }
  return undefined;
}

// Smoke test, test if language server starts up correctly and
// it can distinguish between good and bad syntax.
suite('UDB LS – smoke', () => {
  test('initialize → diagnostics on open (erroneous A.udb file)', async () => {
    // Use your new invalid fixture filename here if you renamed it.
    const uri = vscode.Uri.file(wsPath('AErr.udb'));
    let doc = await vscode.workspace.openTextDocument(uri);
	// force the language in case association is missing.
    if (doc.languageId !== 'udb') {
      doc = await vscode.languages.setTextDocumentLanguage(doc, 'udb');
    }
    await vscode.window.showTextDocument(doc);

    // Nudge validation (on-change + on-save), then revert
    let edit = new vscode.WorkspaceEdit();
    edit.insert(doc.uri, new vscode.Position(0, 0), ' ');
    await vscode.workspace.applyEdit(edit);
    await vscode.workspace.saveAll();
    edit = new vscode.WorkspaceEdit();
    edit.delete(doc.uri, new vscode.Range(0, 0, 0, 1));
    await vscode.workspace.applyEdit(edit);

    const diags = await waitFor(() => {
      const d = vscode.languages.getDiagnostics(doc.uri);
      return d.length ? d : null;
    }, 8000);

	if (!diags || diags.length === 0) {
	  console.log('✓ Found diagnostics for bad file (expected):', diags);
	}
     // Expect at least one diagnostic for the intentionally bad grammar.
    assert.ok(diags && Array.isArray(diags) && diags.length >= 1, 'expected at least one diagnostic for invalid UDB in bad fixture');
  });
  test('initialize → diagnostics on open (erroneous andn.udb file)', async () => {
      // Use your new invalid fixture filename here if you renamed it.
      const uri = vscode.Uri.file(wsPath('andnErr.udb'));
      let doc = await vscode.workspace.openTextDocument(uri);
  	// force the language in case association is missing.
      if (doc.languageId !== 'udb') {
        doc = await vscode.languages.setTextDocumentLanguage(doc, 'udb');
      }
      await vscode.window.showTextDocument(doc);

      // Nudge validation (on-change + on-save), then revert
      let edit = new vscode.WorkspaceEdit();
      edit.insert(doc.uri, new vscode.Position(0, 0), ' ');
      await vscode.workspace.applyEdit(edit);
      await vscode.workspace.saveAll();
      edit = new vscode.WorkspaceEdit();
      edit.delete(doc.uri, new vscode.Range(0, 0, 0, 1));
      await vscode.workspace.applyEdit(edit);

      const diags = await waitFor(() => {
        const d = vscode.languages.getDiagnostics(doc.uri);
        return d.length ? d : null;
      }, 8000);

      if (!diags || diags.length === 0) {
        console.log('✓ Found diagnostics for bad file (expected):', diags);
      }
       // Expect at least one diagnostic for the intentionally bad grammar.
     assert.ok(diags && Array.isArray(diags) && diags.length >= 1, 'expected at least one diagnostic for invalid UDB in bad fixture');
  });
  test('initialize → diagnostics on open (erroneous vsstatus.udb file)', async () => {
	// Use your new invalid fixture filename here if you renamed it.
	const uri = vscode.Uri.file(wsPath('vsstatusErr.udb'));
	let doc = await vscode.workspace.openTextDocument(uri);
	// force the language in case association is missing.
	if (doc.languageId !== 'udb') {
	  doc = await vscode.languages.setTextDocumentLanguage(doc, 'udb');
	}
	await vscode.window.showTextDocument(doc);
	
	// Nudge validation (on-change + on-save), then revert
	let edit = new vscode.WorkspaceEdit();
	edit.insert(doc.uri, new vscode.Position(0, 0), ' ');
	await vscode.workspace.applyEdit(edit);
	await vscode.workspace.saveAll();
	edit = new vscode.WorkspaceEdit();
	edit.delete(doc.uri, new vscode.Range(0, 0, 0, 1));
	await vscode.workspace.applyEdit(edit);
	
	const diags = await waitFor(() => {
	  const d = vscode.languages.getDiagnostics(doc.uri);
	  return d.length ? d : null;
	}, 8000);
	
	if (!diags || diags.length === 0) {
	  console.log('✓ Found diagnostics for bad file (expected):', diags);
	}
	 // Expect at least one diagnostic for the intentionally bad grammar.
	assert.ok(diags && Array.isArray(diags) && diags.length >= 1, 'expected at least one diagnostic for invalid UDB in bad fixture');
  });
  
  test('initialize → diagnostics on open (valid A.udb file)', async () => {
      // Use your new invalid fixture filename here if you renamed it.
      const uri = vscode.Uri.file(wsPath('A.udb'));
      let doc = await vscode.workspace.openTextDocument(uri);
  	// force the language in case association is missing.
      if (doc.languageId !== 'udb') {
        doc = await vscode.languages.setTextDocumentLanguage(doc, 'udb');
      }
      await vscode.window.showTextDocument(doc);

      // Nudge validation (on-change + on-save), then revert
      let edit = new vscode.WorkspaceEdit();
      edit.insert(doc.uri, new vscode.Position(0, 0), ' ');
      await vscode.workspace.applyEdit(edit);
      await vscode.workspace.saveAll();
      edit = new vscode.WorkspaceEdit();
      edit.delete(doc.uri, new vscode.Range(0, 0, 0, 1));
      await vscode.workspace.applyEdit(edit);

      const diags = await waitFor(() => {
        const d = vscode.languages.getDiagnostics(doc.uri);
        return d.length ? d : null;
      }, 8000);

	  if (!diags) {
	    console.log('Diagnostics never arrived (timeout)');
	  } else if (diags.length === 0) {
	    console.log('✓ No diagnostics for valid file (expected)');
	  }
       // Expect no diagnostics for the valid grammar.
	   assert.strictEqual(diags?.length, 0, 'expected no diagnostics for valid UDB in good fixture');
    });
	test('initialize → diagnostics on open (valid andn.udb file)', async () => {
      // Use your new invalid fixture filename here if you renamed it.
      const uri = vscode.Uri.file(wsPath('andn.udb'));
      let doc = await vscode.workspace.openTextDocument(uri);
  	// force the language in case association is missing.
      if (doc.languageId !== 'udb') {
        doc = await vscode.languages.setTextDocumentLanguage(doc, 'udb');
      }
      await vscode.window.showTextDocument(doc);

      // Nudge validation (on-change + on-save), then revert
      let edit = new vscode.WorkspaceEdit();
      edit.insert(doc.uri, new vscode.Position(0, 0), ' ');
      await vscode.workspace.applyEdit(edit);
      await vscode.workspace.saveAll();
      edit = new vscode.WorkspaceEdit();
      edit.delete(doc.uri, new vscode.Range(0, 0, 0, 1));
      await vscode.workspace.applyEdit(edit);

      const diags = await waitFor(() => {
        const d = vscode.languages.getDiagnostics(doc.uri);
        return d.length ? d : null;
      }, 8000);

	  if (!diags) {
	    console.log('Diagnostics never arrived (timeout)');
	  } else if (diags.length === 0) {
	    console.log('✓ No diagnostics for valid file (expected)');
	  }
       // Expect no diagnostics for the valid grammar.
	   assert.strictEqual(diags?.length, 0, 'expected no diagnostics for valid UDB in good fixture');
    });


  // completion test
  test('completion after a keyword (e.g., "kind")', async () => {
    // With the new grammar, keywords include: kind, name, long_name, address, ...
    const doc = await vscode.workspace.openTextDocument({ language: 'udb', content: 'kind ' });
    await vscode.window.showTextDocument(doc);
    const pos = new vscode.Position(0, 'kind '.length);

    const list = await vscode.commands.executeCommand<vscode.CompletionList>(
      'vscode.executeCompletionItemProvider',
      doc.uri,
      pos
    );

    assert.ok(list, 'completion list present');

    assert.ok((list.items ?? []).length >= 1, 'expected some completions after "kind "');
  });

  


});
