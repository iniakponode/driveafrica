from sqlalchemy.orm import Session
from uuid import UUID
from datetime import datetime, timezone
from safedrive.models.embedding import Embedding
from safedrive.schemas.embedding import EmbeddingCreate, EmbeddingUpdate

class EmbeddingCRUD:
    def __init__(self, db: Session):
        self.db = db

    def get(self, embedding_id: UUID) -> Embedding:
        """
        Retrieve an Embedding entity by its ID.

        :param embedding_id: UUID of the Embedding to retrieve.
        :return: Embedding entity if found, else None.
        """
        return self.db.query(Embedding).filter(Embedding.chunkId == embedding_id).first()

    def create(self, embedding_data: EmbeddingCreate) -> Embedding:
        """
        Create a new Embedding entity.

        :param embedding_data: Data required to create a new Embedding.
        :return: The newly created Embedding entity.
        """
        new_embedding = Embedding(
            chunkId=embedding_data.chunkId,
            chunkText=embedding_data.chunkText,
            embedding=embedding_data.embedding,
            sourceType=embedding_data.sourceType,
            sourcePage=embedding_data.sourcePage,
            createdAt=datetime.now(timezone.utc)
        )
        try:
            self.db.add(new_embedding)
            self.db.commit()
            self.db.refresh(new_embedding)
        except Exception as e:
            self.db.rollback()
            raise e
        return new_embedding

    def update(self, embedding_id: UUID, embedding_data: EmbeddingUpdate) -> Embedding:
        """
        Update an existing Embedding entity.

        :param embedding_id: UUID of the Embedding to update.
        :param embedding_data: Data to update the Embedding entity.
        :return: The updated Embedding entity if found, else None.
        """
        existing_embedding = self.get(embedding_id)
        if not existing_embedding:
            return None

        for key, value in embedding_data.dict(exclude_unset=True).items():
            if hasattr(existing_embedding, key):
                setattr(existing_embedding, key, value)

        try:
            self.db.commit()
            self.db.refresh(existing_embedding)
        except Exception as e:
            self.db.rollback()
            raise e
        return existing_embedding

    def delete(self, embedding_id: UUID) -> bool:
        """
        Delete an Embedding entity by its ID.

        :param embedding_id: UUID of the Embedding to delete.
        :return: True if deletion was successful, else False.
        """
        existing_embedding = self.get(embedding_id)
        if not existing_embedding:
            return False

        try:
            self.db.delete(existing_embedding)
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            raise e
        return True

    def get_all(self, skip: int = 0, limit: int = 10) -> list[Embedding]:
        """
        Retrieve a list of Embedding entities.

        :param skip: Number of entities to skip.
        :param limit: Maximum number of entities to retrieve.
        :return: A list of Embedding entities.
        """
        return self.db.query(Embedding).order_by(Embedding.createdAt.desc()).offset(skip).limit(limit).all()

# Instantiate the CRUD utility for usage
embedding_crud = EmbeddingCRUD(db=Session())