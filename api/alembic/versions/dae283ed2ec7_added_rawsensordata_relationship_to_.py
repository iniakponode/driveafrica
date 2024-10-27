"""added rawsensordata relationship to location

Revision ID: dae283ed2ec7
Revises: 41d35a8a8f2a
Create Date: 2024-10-14 17:17:19.413828

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'dae283ed2ec7'
down_revision: Union[str, None] = '41d35a8a8f2a'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
