package org.xtext.example.udb.naming;

import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
// import your actual target model type here
import org.xtext.example.udb.udb.CsrModel;
import org.xtext.example.udb.udb.InstModel;
import org.xtext.example.udb.udb.ExtModel;


public class UdbQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	protected QualifiedName qualifiedName(CsrModel csr) {
		if (csr.getCsrName() == null || csr.getCsrName().getName() == null) {
			return null;
		}
		return QualifiedName.create(csr.getCsrName().getName());
	}
	
	protected QualifiedName qualifiedName(InstModel inst) {
		if (inst.getInstName() == null || inst.getInstName().getName() == null) {
			return null;
		}
		return QualifiedName.create(inst.getInstName().getName());
	}
	
	protected QualifiedName qualifiedName(ExtModel ext) {
		if (ext.getExtName() == null || ext.getExtName().getName() == null) {
			return null;
		}
		return QualifiedName.create(ext.getExtName().getName());
	}

}