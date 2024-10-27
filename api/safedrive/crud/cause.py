from sqlalchemy.orm import Session
from uuid import UUID
from datetime import datetime
from safedrive.models.cause import Cause
from safedrive.schemas.cause import CauseCreate, CauseUpdate

class CauseCRUD:
    def __init__(self, db: Session):
        self.db = db

    def get(self, cause_id: UUID) -> Cause:
        with self.db as session:
            return session.query(Cause).filter(Cause.id == cause_id).first()

    def create(self, cause_data: CauseCreate) -> Cause:
        with self.db as session:
            new_cause = Cause(
                id=cause_data.id,
                unsafeBehaviourId=cause_data.unsafeBehaviourId,
                name=cause_data.name,
                influence=cause_data.influence,
                createdAt=datetime.utcnow(),
                updatedAt=None
            )
            try:
                session.add(new_cause)
                session.commit()
                session.refresh(new_cause)
            except Exception as e:
                session.rollback()
                raise e
            return new_cause

    def update(self, cause_id: UUID, cause_data: CauseUpdate) -> Cause:
        with self.db as session:
            existing_cause = session.query(Cause).filter(Cause.id == cause_id).first()
            if not existing_cause:
                return None

            for key, value in cause_data.dict(exclude_unset=True).items():
                setattr(existing_cause, key, value)
            existing_cause.updatedAt = datetime.utcnow()

            try:
                session.commit()
                session.refresh(existing_cause)
            except Exception as e:
                session.rollback()
                raise e
            return existing_cause

    def delete(self, cause_id: UUID) -> bool:
        with self.db as session:
            existing_cause = session.query(Cause).filter(Cause.id == cause_id).first()
            if not existing_cause:
                return False

            try:
                session.delete(existing_cause)
                session.commit()
            except Exception as e:
                session.rollback()
                raise e
            return True

    def get_all(self, skip: int = 0, limit: int = 10) -> list[Cause]:
        with self.db as session:
            return session.query(Cause).offset(skip).limit(limit).all()

# Instantiate the CRUD utility for cause operations
cause_crud = CauseCRUD(db=Session())