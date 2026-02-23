package com.app.docflow.application.document.command;

import com.app.docflow.application.document.command.model.CreateDocumentCommand;
import com.app.docflow.domain.document.Document;

public interface CreateDocumentUseCase {

    Document create(CreateDocumentCommand command);

}
